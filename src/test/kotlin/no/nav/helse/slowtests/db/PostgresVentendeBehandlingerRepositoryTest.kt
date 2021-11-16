package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.slowtests.KoinTestBase
import no.nav.helse.slowtests.clearAllDatabaseTables
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class PostgresVentendeBehandlingerRepositoryTest : KoinTestBase() {

    lateinit var repo: PostgresVentendeBehandlingerRepository
    lateinit var dataSource: HikariDataSource

    private val msg = SpleisInntektsmeldingMelding(
        "123456785",
        LocalDate.now(),
        LocalDate.now().plusDays(1),
        LocalDateTime.now(),
        "123"
    )

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresVentendeBehandlingerRepository(dataSource)
    }

    @Test
    internal fun `kan inserte og finne`() {
        repo.insertIfNotExists(msg.fødselsnummer, msg.organisasjonsnummer, msg.fom, msg.tom, msg.opprettet)

        val result = repo.findOlderThan(msg.opprettet)

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(msg)
    }

    @Test
    internal fun `kan fjerne når spleis ikke lenger venter på IM`() {
        repo.insertIfNotExists(msg.fødselsnummer, msg.organisasjonsnummer, msg.fom, msg.tom, msg.opprettet)
        repo.remove(msg.fødselsnummer, msg.organisasjonsnummer, dataSource.connection)

        val result = repo.findOlderThan(msg.opprettet)

        assertThat(result).isEmpty()
    }

    @Test
    internal fun `kan inserte samme fnr, vnr og fom uten å få duplikatperiode og uten å endre eksisternde periode`() {
        repo.insertIfNotExists(msg.fødselsnummer, msg.organisasjonsnummer, msg.fom, msg.tom, msg.opprettet)
        repo.insertIfNotExists(msg.fødselsnummer, msg.organisasjonsnummer, msg.fom, LocalDate.now(), LocalDateTime.now())

        val result = repo.findOlderThan(msg.opprettet)

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(msg)
    }

    @AfterEach
    internal fun tearDown() {
        clearAllDatabaseTables()
    }
}
