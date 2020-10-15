package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptExternal
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokumentResponse
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.inntektsmeldingsvarsel.AltinnVarselSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSenderImpl.Companion.SYSTEM_USER_CODE
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AltinnBrevutsendelseSenderImplTest {
    val joarkMock = mockk<DokarkivKlient>()
    val brevmalRepoMock = mockk<AltinnBrevMalRepository>()
    val soapClientMock = mockk<ICorrespondenceAgencyExternalBasic>()

    private val username = "test-username"
    private val password = "test-password"

    private val response = JournalpostResponse(
            journalpostId = "12345",
            journalpostFerdigstilt = true,
            journalStatus = "J",
            dokumenter = listOf(DokumentResponse(null,null,null))
    )

    val altinnSender = AltinnBrevutsendelseSenderImpl(
            joarkMock,
            brevmalRepoMock,
            soapClientMock,
            username,
            password
    )

    @Test
    fun `Henter mal, sender og journalfører`() {
        every { joarkMock.journalførDokument(any(), any(), any()) } returns response
        every { brevmalRepoMock.get(TestData.AltinnBrevmal.id) } returns TestData.AltinnBrevmal
        every { soapClientMock.insertCorrespondenceBasicV2(any(), any(), any(), any(), any()) } returns ReceiptExternal().withReceiptStatusCode(ReceiptStatusEnum.OK)

        altinnSender.send(TestData.AltinnBrevutsendelse_Ubehandlet)

        verify(exactly = 1) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { brevmalRepoMock.get(TestData.AltinnBrevmal.id) }
        verify(exactly = 1) {
            soapClientMock.insertCorrespondenceBasicV2(
                    username,
                    password,
                    AltinnBrevutsendelseSenderImpl.SYSTEM_USER_CODE,
                    any(),
                    any())
        }
    }

    @Test
    fun `Feiler ved ikke-ok status fra Altinn`() {
        every { joarkMock.journalførDokument(any(), any(), any()) } returns response
        every { brevmalRepoMock.get(TestData.AltinnBrevmal.id) } returns TestData.AltinnBrevmal
        every { soapClientMock.insertCorrespondenceBasicV2(any(), any(), any(), any(), any()) } returns ReceiptExternal().withReceiptStatusCode(ReceiptStatusEnum.UN_EXPECTED_ERROR)

        assertThrows<IllegalStateException> { altinnSender.send(TestData.AltinnBrevutsendelse_Ubehandlet) }

        verify(exactly = 0) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { brevmalRepoMock.get(TestData.AltinnBrevmal.id) }
        verify(exactly = 1) {
            soapClientMock.insertCorrespondenceBasicV2(
                    username,
                    password,
                    AltinnBrevutsendelseSenderImpl.SYSTEM_USER_CODE,
                    any(),
                    any())
        }
    }
}
