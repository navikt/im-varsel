package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.slowtests.KoinTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PostgresAltinnBrevUtsendelseRepositoryTest : KoinTestBase() {

    lateinit var repo: PostgresAltinnBrevUtsendelseRepository
    lateinit var dataSource: HikariDataSource

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresAltinnBrevUtsendelseRepository(dataSource)
    }

    @Test
    internal fun `kan inserte og hente usendte og oppdatere sendestatus`() {
        repo.insertUtsendelse(UUID.randomUUID(), setOf("123456789", "985746364", "984759123"))

        val batch = repo.getNextBatch()

        assertThat(batch).hasSize(3)

        batch.forEach { repo.updateSentStatus(it.id, LocalDateTime.now(), true) }

        val batchAfterSentStatusUpdate = repo.getNextBatch()

        assertThat(batchAfterSentStatusUpdate).hasSize(0)
    }
}
