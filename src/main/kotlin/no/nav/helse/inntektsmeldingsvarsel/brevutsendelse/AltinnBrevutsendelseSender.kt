package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import no.nav.helse.inntektsmeldingsvarsel.pdf.PDFGenerator
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMal
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtesendelse
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.*

class AltinnBrevutsendelseSender(
        private val joarkClient: DokarkivKlientImpl,
        private val malRepo: AltinnBrevMalRepository,
        private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
        private val username: String,
        private val password: String) {

    private val log = LoggerFactory.getLogger("AltinnBrevutsendelseSender")
    private val brevMalCache = SimpleHashMapCache<AltinnBrevMal>(
            cacheDuration = Duration.ofHours(1),
            maxCachedItems = 2
    )
    private val pdfGenerator = PDFGenerator()

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    fun send(utsendelse: AltinnBrevUtesendelse) {
        val mal = if (brevMalCache.hasValidCacheEntry(utsendelse.altinnBrevMalId.toString()))
                        brevMalCache.get(utsendelse.altinnBrevMalId.toString())
                else malRepo.get(utsendelse.altinnBrevMalId)


        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, utsendelse.id.toString(),
                    mapToAltinnMessageFormat(utsendelse, mal)
            )

            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw IllegalStateException("Feil ved sending av brev til Altinn")
            }

            journalfør(utsendelse, mal)

        } catch (e: Exception) {
            log.error("Feil ved sending av brev til Altinn", e)
            throw e
        }
    }


    fun journalfør(brevutsendelse: AltinnBrevUtesendelse, brevmal: AltinnBrevMal): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(brevmal))

        val request = JournalpostRequest(
                tema = brevmal.joarkTema,
                journalposttype = Journalposttype.UTGAAENDE,
                kanal = "ALTINN",
                tittel = brevmal.joarkTittel,
                bruker = Bruker(brevutsendelse.virksomhetsNr, IdType.ORGNR),
                eksternReferanseId = "helsearbeisgiver-altinn-ut-${brevutsendelse.id}",
                avsenderMottaker = AvsenderMottaker(
                        brevutsendelse.virksomhetsNr,
                        IdType.ORGNR,
                        "Arbeidsgiver"
                ),
                dokumenter = listOf(Dokument(
                        tittel = brevmal.joarkTittel,
                        brevkode = brevmal.joarkBrevkode,
                        dokumentVarianter = listOf(DokumentVariant(
                                fysiskDokument = base64EnkodetPdf
                        ))
                )),
                datoMottatt = LocalDate.now()
        )

        val joarkRef = joarkClient.journalførDokument(request, true, UUID.randomUUID().toString())

        log.info("Journalført ${brevutsendelse.id} med respons $joarkRef")
        return joarkRef.journalpostId
    }

    fun mapToAltinnMessageFormat(utsendelse: AltinnBrevUtesendelse, mal: AltinnBrevMal): InsertCorrespondenceV2 {
        val meldingsInnhold = ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(mal.header)
                .withMessageBody(mal.bodyHtml)
                .withMessageSummary(mal.summary)

        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(utsendelse.virksomhetsNr)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(mal.altinnTjenestekode)
                .withServiceEdition(mal.altinnTjenesteVersjon)
                .withContent(meldingsInnhold)
    }


}