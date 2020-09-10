package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingDbEntity
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.time.LocalDateTime
import java.util.*

internal class PostgresVarslingRepositoryTest : KoinComponent {

    lateinit var repo: PostgresVarslingRepository
    lateinit var dataSource: HikariDataSource

    private val dbVarsling = VarslingDbEntity(
            uuid = UUID.randomUUID().toString(),
            data = "[]",
            sent = false,
            read = false,
            opprettet = LocalDateTime.now(),
            aggregatperiode = "D-2020-01-01",
            virksomhetsNr = "123456789"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresVarslingRepository(dataSource)
        repo.insert(dbVarsling)
    }

    @Test
    internal fun `kan inserte og hente`() {
        val fradb = repo.findByVirksomhetsnummerAndPeriode(dbVarsling.virksomhetsNr, dbVarsling.aggregatperiode)

        assertThat(fradb).isEqualTo(dbVarsling)
    }

    @Test
    internal fun `kan oppdatere data`() {
        val timeOfUpdate = LocalDateTime.now()
        repo.updateSentStatus(dbVarsling.uuid, timeOfUpdate, true)
        val afterUpdate = repo.findByVirksomhetsnummerAndPeriode(dbVarsling.virksomhetsNr, dbVarsling.aggregatperiode)

        assertThat(afterUpdate?.behandlet).isEqualTo(timeOfUpdate)
        assertThat(afterUpdate?.sent).isEqualTo(true)
    }

    @Test
    internal fun `kan oppdatere sendt status`() {
        val timeOfUpdate = LocalDateTime.now()

        repo.updateSentStatus(dbVarsling.uuid, timeOfUpdate, true)

        val afterUpdate = repo.findByVirksomhetsnummerAndPeriode(dbVarsling.virksomhetsNr, dbVarsling.aggregatperiode)

        assertThat(afterUpdate?.behandlet).isEqualTo(timeOfUpdate)
        assertThat(afterUpdate?.sent).isEqualTo(true)
    }

    @Test
    internal fun `kan oppdatere lest status`() {
        repo.updateReadStatus(dbVarsling.uuid, true)

        val afterUpdate = repo.findByVirksomhetsnummerAndPeriode(dbVarsling.virksomhetsNr, dbVarsling.aggregatperiode)

        assertThat(afterUpdate?.read).isEqualTo(true)
    }

    @AfterEach
    internal fun tearDown() {
        repo.remove(dbVarsling.uuid)
        stopKoin()
    }
}