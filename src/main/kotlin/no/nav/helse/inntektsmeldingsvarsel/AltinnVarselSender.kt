package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.pdf.PDFGenerator
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingSender
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingService
import org.slf4j.LoggerFactory
import java.util.*

class AltinnVarselSender(
        private val dokarkivKlient: DokarkivKlient,
        private val altinnVarselMapper: AltinnVarselMapper,
        private val varslingService: VarslingService,
        private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
        private val username: String,
        private val password: String) : VarslingSender {

    private val log = LoggerFactory.getLogger("AltinnVarselSender")

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
        val pdfGenerator = PDFGenerator()
    }

    override fun send(varsling: Varsling) {

        try {
            varsling.liste.forEach {
                if (it.joarkRef == null) {
                    it.joarkRef = journalførEnkeltVarsel(varsling, it)
                    varslingService.oppdaterData(varsling)
                    log.info("Journalført enkeltvarsling med joarRef {}",it.joarkRef)
                }
            }

            journalfør(varsling)

            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, varsling.uuid,
                    altinnVarselMapper.mapVarslingTilInsertCorrespondence(varsling)
            )

            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw IllegalStateException("Feil ved sending av varsel om manglende innsending av inntektsmelding til Altinn")
            }

            ANTALL_SENDTE_VARSLER.inc()
            ANTALL_PERSONER_I_SENDTE_VARSLER.inc(varsling.liste.size.toDouble())

        } catch (e: Exception) {
            log.error("Feil ved sending varsel om manglende innsending av inntektsmelding til Altinn", e)
            throw e
        }
    }

    private val tittel = "Varsel om manglende inntektsmelding"

    fun journalfør(varsel: Varsling): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel, varsel.liste))

        val response = dokarkivKlient.journalførDokument(
                JournalpostRequest(
                        tittel = tittel,
                        journalposttype = Journalposttype.UTGAAENDE,
                        kanal = "ALTINN",
                        bruker = Bruker(varsel.virksomhetsNr, IdType.ORGNR),
                        eksternReferanseId = varsel.uuid,
                        avsenderMottaker = AvsenderMottaker(
                                id = varsel.virksomhetsNr,
                                idType = IdType.ORGNR,
                                navn = "Arbeidsgiver"
                        ),
                        dokumenter = listOf(Dokument(
                                dokumentVarianter = listOf(DokumentVariant(
                                        fysiskDokument = base64EnkodetPdf
                                )),
                                brevkode = "varsel_om_manglende_inntektsmelding",
                                tittel = tittel,
                        )),
                        datoMottatt = varsel.opprettet.toLocalDate()
                ), true, UUID.randomUUID().toString()

        )

        log.debug("Journalført ${varsel.uuid} med ref ${response.journalpostId}")
        return response.journalpostId
    }

    fun journalførEnkeltVarsel(varsel: Varsling, personVarsel: PersonVarsling): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel, setOf(personVarsel)))
        val eksternrefId = varsel.uuid +  "-" + varsel.liste.indexOfFirst { it.personnumer == personVarsel.personnumer }

        val response = dokarkivKlient.journalførDokument(
                JournalpostRequest(
                        tittel = tittel,
                        journalposttype = Journalposttype.UTGAAENDE,
                        kanal = "ALTINN",
                        bruker = Bruker(personVarsel.personnumer, IdType.FNR),
                        eksternReferanseId = eksternrefId,
                        avsenderMottaker = AvsenderMottaker(
                                id = varsel.virksomhetsNr,
                                idType = IdType.ORGNR,
                                navn = "Arbeidsgiver"
                        ),
                        dokumenter = listOf(Dokument(
                                dokumentVarianter = listOf(DokumentVariant(
                                        fysiskDokument = base64EnkodetPdf
                                )),
                                brevkode = "varsel_om_manglende_inntektsmelding",
                                tittel = tittel,
                        )),
                        datoMottatt = varsel.opprettet.toLocalDate()
                ), true, UUID.randomUUID().toString()

        )

        log.debug("Journalført varsel for enkeltperson på ${eksternrefId} med ref ${response.journalpostId}")

        return response.journalpostId
    }
}
