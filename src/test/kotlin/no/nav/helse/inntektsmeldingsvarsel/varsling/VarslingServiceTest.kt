package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.PilotAllowList
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding.Meldingstype.TRENGER_IKKE_INNTEKTSMELDING
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingServiceTest {
    val allowMock = mockk<PilotAllowList>()
    val varselRepo = mockk<VarslingRepository>(relaxed = true)
    val ventendeRepoMock = mockk<VentendeBehandlingerRepository>(relaxed = true)
    val altinnVarselMapperMock = mockk<VarslingMapper>()
    val datasourceMock = mockk<HikariDataSource>(relaxed = true)

    val pdlClientMock = mockk<PdlClient>() {
        every { personNavn(any()) } returns PdlHentPersonNavn.PdlPersonNavneliste(listOf(
            PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                "Navn",
                null,

                "Navnesen",
                metadata = PdlPersonNavnMetadata("FREG")
            )
        ))
    }

    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

    val serviceUnderTest = VarslingService(
            datasourceMock,
            varselRepo,
            ventendeRepoMock,
            altinnVarselMapperMock,
            objectMapper,
            pdlClientMock,
            allowMock
    )

    private val msg_mangler = SpleisInntektsmeldingMelding(
             "123456785",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            LocalDateTime.now(),
            "123"
    )

    @Test
    fun `Innkommende meldinger fra Spleis blir lagret i ventende tabellen`() {
        serviceUnderTest.handleMessage(objectMapper.writeValueAsString(msg_mangler))

        verify(exactly = 1) { ventendeRepoMock.insertIfNotExists(msg_mangler.fødselsnummer, msg_mangler.organisasjonsnummer, msg_mangler.fom, msg_mangler.tom, msg_mangler.opprettet) }
        verify(exactly = 0) { varselRepo.insert(any(), any()) }
    }

    @Test
    fun `Melding om at man ikke lenger mangler IM fjerner ventende melding`() {
        val trengerIkke = msg_mangler.copy(type = TRENGER_IKKE_INNTEKTSMELDING)
        serviceUnderTest.handleMessage(objectMapper.writeValueAsString(trengerIkke))

        verify(exactly = 1) { ventendeRepoMock.remove(trengerIkke.fødselsnummer, trengerIkke.organisasjonsnummer, any()) }
        verify(exactly = 0) { ventendeRepoMock.insertIfNotExists(msg_mangler.fødselsnummer, msg_mangler.organisasjonsnummer, msg_mangler.fom, msg_mangler.tom, msg_mangler.opprettet) }
        verify(exactly = 0) { varselRepo.insert(any(), any()) }
    }

    @Test
    fun `Oppretter ikke varsler og fjerner ventende melding ved org-nummer som ikke er i allow list`() {
        every { allowMock.isAllowed(msg_mangler.organisasjonsnummer) } returns false
        every { ventendeRepoMock.findOlderThan(any()) } returns setOf(msg_mangler)

        serviceUnderTest.opprettVarslingerFraVentendeMeldinger()

        verify(exactly = 1) { allowMock.isAllowed(msg_mangler.organisasjonsnummer) }
        verify(exactly = 1) { ventendeRepoMock.remove(msg_mangler.fødselsnummer, msg_mangler.organisasjonsnummer, any()) }
        verify(exactly = 0) { varselRepo.insert(any(), any()) }
    }
}