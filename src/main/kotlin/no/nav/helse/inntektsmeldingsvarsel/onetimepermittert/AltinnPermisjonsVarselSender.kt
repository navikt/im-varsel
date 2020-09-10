package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.joark.PDFGenerator
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlientImpl
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsVarselDbEntity
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingSender
import org.slf4j.LoggerFactory
import java.util.*

class AltinnPermisjonsVarselSender(
        private val joarkClient: DokarkivKlientImpl,
        private val altinnVarselMapper: AltinnPermisjonsVarselMapper,
        private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
        private val username: String,
        private val password: String) {

    private val log = LoggerFactory.getLogger("AltinnPermisjonsVarselSender")

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
        val pdfGenerator = PDFGenerator()
    }

    fun send(varsling: PermisjonsVarselDbEntity) {

        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, "nav-covid-perm-feil-${varsling.virksomhetsNr}",
                    altinnVarselMapper.mapVarslingTilInsertCorrespondence(varsling)
            )

            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw IllegalStateException("Feil ved sending av varsel om manglende innsending av inntektsmelding til Altinn")
            }

            journalfør(varsling)

        } catch (e: Exception) {
            log.error("Feil ved sending varsel om manglende innsending av inntektsmelding til Altinn", e)
            throw e
        }
    }


    fun journalfør(varsel: PermisjonsVarselDbEntity): String {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(varsel))
        val joarkRef = joarkClient.journalførDokument(base64EnkodetPdf, varsel, UUID.randomUUID().toString())
        log.info("Journalført ${varsel.id} med ref $joarkRef")
        return joarkRef
    }
}