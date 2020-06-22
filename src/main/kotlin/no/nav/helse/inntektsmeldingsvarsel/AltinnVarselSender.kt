package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.joark.PDFGenerator
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingSender
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingService
import org.slf4j.LoggerFactory
import java.util.*

class AltinnVarselSender(
        private val allowList: AllowList,
        private val joarkClient: DokarkivKlient,
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

        if (!allowList.shouldSend(varsling.virksomhetsNr)) {
            return
        }

        try {

            varsling.liste.forEach {
                if (!it.journalført) {
                    journalførEnkeltVarsel(varsling, it)
                    it.journalført = true
                    varslingService.oppdaterData(varsling)
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

    fun journalfør(varsel: Varsling): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel, varsel.liste))
        val joarkRef = joarkClient.journalførDokument(base64EnkodetPdf, varsel, UUID.randomUUID().toString(), varsel.virksomhetsNr, "ORGNR")
        log.info("Journalført ${varsel.uuid} med ref $joarkRef")
        return joarkRef
    }

    fun journalførEnkeltVarsel(varsel: Varsling, personVarsel: PersonVarsling): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel, setOf(personVarsel)))
        val joarkRef = joarkClient.journalførDokument(base64EnkodetPdf, varsel, UUID.randomUUID().toString(), personVarsel.personnumer, "FNR")
        log.info("Journalført varsel for enkeltperson på: ${varsel.uuid}  med ref $joarkRef")
        return joarkRef
    }
}
