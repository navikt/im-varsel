package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavn
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingDbEntity
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

internal class VarslingServiceTest {
    val existingVarselAggregat = Varsling(
            "123456785",
            mutableSetOf()
    )

    val pdlPerson = PdlPerson(listOf(PdlPersonNavn("Navn", null, "Navnesen")))

    val mappingResultDto = VarslingDbEntity(data = "[]", uuid = "test", read = false,
            sent = false, opprettet = LocalDateTime.now(), behandlet = LocalDateTime.now(), virksomhetsNr = "12345")

    val allowMock = mockk<AllowList>()
    val varselRepo = mockk<VarslingRepository>(relaxed = true)
    val ventendeRepoMock = mockk<VentendeBehandlingerRepository>(relaxed = true)
    val altinnVarselMapperMock = mockk<VarslingMapper>()
    val datasourceMock = mockk<DataSource>()
    val pdlClientMock = mockk<PdlClient>()
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

    private val varsling = SpleisInntektsmeldingMelding(
             "123456785",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            LocalDateTime.now(),
            "123"
    )

    @Test
    fun `Ignorerer meldinger ang org-nummer som ikke er i allow list`() {
        every { allowMock.isAllowed(varsling.organisasjonsnummer) } returns false

        serviceUnderTest.handleMessage(objectMapper.writeValueAsString(varsling))

        verify(exactly = 1) { allowMock.isAllowed(varsling.organisasjonsnummer) }
        verify(exactly = 0) { ventendeRepoMock.insertIfNotExists(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { varselRepo.updateData(any(), any()) }
    }

}