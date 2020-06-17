package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.joark.PDFGenerator
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingSender
import org.slf4j.LoggerFactory
import java.util.*

class AltinnVarselSender(
        private val allowList: AllowList,
        private val joarkClient: DokarkivKlient,
        private val altinnVarselMapper: AltinnVarselMapper,
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
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel))
        val joarkRef = joarkClient.journalførDokument(base64EnkodetPdf, varsel, UUID.randomUUID().toString())
        log.info("Journalført ${varsel.uuid} med ref $joarkRef")
        return joarkRef
    }
}