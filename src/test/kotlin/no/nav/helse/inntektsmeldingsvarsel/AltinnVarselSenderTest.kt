package no.nav.helse.inntektsmeldingsvarsel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptExternal
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokumentResponse
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class AltinnVarselSenderTest {
    val joarkMock = mockk<DokarkivKlient>()
    val serviceMock = mockk<VarslingService>(relaxed = true)
    val altinnVarselMapperMock = mockk<AltinnVarselMapper>()
    val soapClientMock = mockk<ICorrespondenceAgencyExternalBasic>()

    private val username = "test-username"
    private val password = "test-password"

    private val response = JournalpostResponse(
        journalpostId = "12345",
        journalpostFerdigstilt = true,
        journalStatus = "J",
        dokumenter = listOf(DokumentResponse(null, null, null))
    )

    val altinnSender = AltinnVarselSender(
        joarkMock,
        altinnVarselMapperMock,
        serviceMock,
        soapClientMock,
        username,
        password
    )

    private val varsling = Varsling(
        virksomhetsNr = "123456785",
        virksomhetsNavn = "Stark Industries",
        liste = mutableSetOf(PersonVarsling("Ole", "123", Periode(LocalDate.now(), LocalDate.now()), LocalDateTime.now()))
    )

    val mappedSoapMessage = InsertCorrespondenceV2()

    @Test
    fun `Sjekker allowlist, journalfører og sender`() {
        every { joarkMock.journalførDokument(any(), any(), any()) } returns response
        every { altinnVarselMapperMock.mapVarslingTilInsertCorrespondence(varsling) } returns mappedSoapMessage
        every { soapClientMock.insertCorrespondenceBasicV2(any(), any(), any(), any(), any()) } returns ReceiptExternal().withReceiptStatusCode(ReceiptStatusEnum.OK)

        altinnSender.send(varsling)

        verify(exactly = 2) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { altinnVarselMapperMock.mapVarslingTilInsertCorrespondence(varsling) }
        verify(exactly = 1) {
            soapClientMock.insertCorrespondenceBasicV2(
                username,
                password,
                AltinnVarselSender.SYSTEM_USER_CODE,
                varsling.uuid,
                mappedSoapMessage
            )
        }
    }

    @Test
    fun `Feiler ved ikke-ok status fra Altinn`() {
        every { joarkMock.journalførDokument(any(), any(), any()) } returns response
        every { altinnVarselMapperMock.mapVarslingTilInsertCorrespondence(varsling) } returns mappedSoapMessage
        every { soapClientMock.insertCorrespondenceBasicV2(any(), any(), any(), any(), any()) } returns ReceiptExternal().withReceiptStatusCode(ReceiptStatusEnum.UN_EXPECTED_ERROR)

        assertThrows<IllegalStateException> { altinnSender.send(varsling) }

        verify(exactly = 2) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { altinnVarselMapperMock.mapVarslingTilInsertCorrespondence(varsling) }
        verify(exactly = 1) {
            soapClientMock.insertCorrespondenceBasicV2(
                username,
                password,
                AltinnVarselSender.SYSTEM_USER_CODE,
                varsling.uuid,
                mappedSoapMessage
            )
        }
    }
}
