package no.nav.helse.inntektsmeldingsvarsel

import com.fasterxml.jackson.databind.ObjectMapper
import no.altinn.schemas.services.serviceengine.correspondence._2016._02.CorrespondenceStatusFilterV3
import no.altinn.schemas.services.serviceengine.correspondence._2016._02.SdpStatusSearchOptions
import no.altinn.schemas.services.serviceentity._2014._10.CorrespondenceStatusTypeV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.varsling.ReadReceiptProvider
import org.slf4j.LoggerFactory

class AltinnReadReceiptClient(private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
                              private val username: String,
                              private val password: String,
                              private val altinnSerivceCode: String,
                              private val om: ObjectMapper) : ReadReceiptProvider {

    private val log = LoggerFactory.getLogger("AltinnReadReceiptClient")

    private val altinnReadStates = setOf(CorrespondenceStatusTypeV2.READ, CorrespondenceStatusTypeV2.CONFIRMED)

    override fun isRead(varsel: Varsling): Boolean {
        try {
            val query = CorrespondenceStatusFilterV3()
                    .withSendersReference(varsel.uuid)
                    .withServiceCode(altinnSerivceCode)
                    .withServiceEditionCode(1)
                    .withReportee(varsel.virksomhetsNr)
                    .withSdpSearchOptions(SdpStatusSearchOptions().withIncludeCorrespondence(true))

            val receiptExternal = iCorrespondenceAgencyExternalBasic.getCorrespondenceStatusDetailsBasicV3(
                    username, password, query)

            log.trace(om.writeValueAsString(receiptExternal))

            val status = receiptExternal.correspondenceStatusInformation.correspondenceStatusDetailsList.statusV2.firstOrNull()?.statusChanges?.statusChangeV2?.lastOrNull()?.statusType

            log.debug("Found altinn read status $status")

            return altinnReadStates.contains(status)
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            throw RuntimeException("Feil ved henting av leststatus fra Altinn", e)
        } catch (e: Exception) {
            throw e
        }
    }
}