package no.nav.helse.inntektsmeldingsvarsel.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.inntektsmeldingsvarsel.AllowList
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.MeldingsfilterRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingDbEntity
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlClient
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlHentPerson
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlPerson
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlPersonNavn
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsMeldingMelding
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingServiceTest {
    val existingVarselAggregat = Varsling(
            "D-2020-01-01",
            "123456785",
            mutableSetOf()
    )

    val pdlPerson = PdlPerson(listOf(PdlPersonNavn("Navn", null, "Navnesen")), null)

    val mappingResultDto = VarslingDbEntity(data = "[]", uuid = "test", read = false,
            sent = false, opprettet = LocalDateTime.now(), behandlet = LocalDateTime.now(), aggregatperiode = "D-2020", virksomhetsNr = "12345")

    val allowMock = mockk<AllowList>()
    val varselRepo = mockk<VarslingRepository>(relaxed = true)
    val hashRepo = mockk<MeldingsfilterRepository>(relaxed = true)
    val altinnVarselMapperMock = mockk<VarslingMapper>()
    val pdlClientMock = mockk<PdlClient>()
    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

    val serviceUnderTest = VarslingService(
            varselRepo,
            altinnVarselMapperMock,
            objectMapper,
            hashRepo,
            pdlClientMock,
            allowMock
    )

    private val varsling = ManglendeInntektsMeldingMelding(
             "123456785",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            LocalDateTime.now(),
            "123"
    )

    @Test
    fun `Ignorerer meldinger ang org-nummer som ikke er i allow list`() {
        every { allowMock.isAllowed(varsling.organisasjonsnummer) } returns false

        serviceUnderTest.aggregate(objectMapper.writeValueAsString(varsling))

        verify(exactly = 1) { allowMock.isAllowed(varsling.organisasjonsnummer) }
        verify(exactly = 0) { varselRepo.insert(any()) }
        verify(exactly = 0) { varselRepo.updateData(any(), any()) }
    }

    @Test
    fun `Ignorerer meldinger som er duplikater`() {
        every { allowMock.isAllowed(varsling.organisasjonsnummer) } returns true
        every { hashRepo.exists(any()) } returns true

        serviceUnderTest.aggregate(objectMapper.writeValueAsString(varsling))

        verify(exactly = 1) { allowMock.isAllowed(varsling.organisasjonsnummer) }
        verify(exactly = 1) { hashRepo.exists(varsling.periodeHash()) }
        verify(exactly = 0) { varselRepo.insert(any()) }
        verify(exactly = 0) { varselRepo.updateData(any(), any()) }
    }

    @Test
    fun `Oppretter nytt aggregat og logger i hashtabellen hvis ingen finnes fra før`() {
        every { allowMock.isAllowed(varsling.organisasjonsnummer) } returns true
        every { hashRepo.exists(any()) } returns false
        every { varselRepo.findByVirksomhetsnummerAndPeriode(any(), any()) } returns null
        every { pdlClientMock.person(any()) } returns pdlPerson
        every { altinnVarselMapperMock.mapDto(any())} returns mappingResultDto

        serviceUnderTest.aggregate(objectMapper.writeValueAsString(varsling))

        verify(exactly = 1) { allowMock.isAllowed(varsling.organisasjonsnummer) }
        verify(exactly = 1) { hashRepo.exists(varsling.periodeHash()) }
        verify(exactly = 1) { pdlClientMock.person(any()) }
        verify(exactly = 1) { varselRepo.insert(any()) }
        verify(exactly = 1) { hashRepo.insert(varsling.periodeHash()) }
        verify(exactly = 0) { varselRepo.updateData(any(), any()) }
    }

    @Test
    fun `Oppdaterer eksisterende aggregat og logger i hashtabellen hvis aggregat finnes fra før`() {
        every { allowMock.isAllowed(varsling.organisasjonsnummer) } returns true
        every { hashRepo.exists(any()) } returns false
        every { varselRepo.findByVirksomhetsnummerAndPeriode(any(), any()) } returns mappingResultDto
        every { pdlClientMock.person(any()) } returns pdlPerson
        every { altinnVarselMapperMock.mapDomain(any())} returns existingVarselAggregat
        every { altinnVarselMapperMock.mapDto(any())} returns mappingResultDto

        serviceUnderTest.aggregate(objectMapper.writeValueAsString(varsling))

        verify(exactly = 1) { allowMock.isAllowed(varsling.organisasjonsnummer) }
        verify(exactly = 1) { hashRepo.exists(varsling.periodeHash()) }
        verify(exactly = 1) { pdlClientMock.person(any()) }
        verify(exactly = 0) { varselRepo.insert(any()) }
        verify(exactly = 1) { hashRepo.insert(varsling.periodeHash()) }
        verify(exactly = 1) { varselRepo.updateData(existingVarselAggregat.uuid, mappingResultDto.data) }
    }

}