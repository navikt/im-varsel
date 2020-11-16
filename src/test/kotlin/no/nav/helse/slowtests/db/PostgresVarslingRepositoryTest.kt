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
            virksomhetsNr = "123456789"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresVarslingRepository(dataSource)
        repo.insert(dbVarsling, dataSource.connection)
    }

    @Test
    internal fun `kan inserte og hente`() {
        val fradb = repo.findBySentStatus(false,1)

        assertThat(fradb).isEqualTo(dbVarsling)
    }

    @Test
    internal fun `kan oppdatere jsonData`() {
        val jsonData = "[]"
        repo.updateData(dbVarsling.uuid, jsonData)
        val afterUpdate = repo.findBySentStatus(false, 1)[0]

        assertThat(afterUpdate?.data).isEqualTo(jsonData)
    }

    @Test
    internal fun `kan oppdatere sendt status og finne fra status`() {
        val timeOfUpdate = LocalDateTime.now()

        repo.updateSentStatus(dbVarsling.uuid, timeOfUpdate, true)

        val allSent = repo.findBySentStatus(true, 1)

        assertThat(allSent).hasSize(1)

        val afterUpdate = allSent.first()

        assertThat(afterUpdate?.behandlet).isEqualTo(timeOfUpdate)
        assertThat(afterUpdate?.sent).isEqualTo(true)
    }

    @Test
    internal fun `kan oppdatere lest status`() {
        repo.updateReadStatus(dbVarsling.uuid, true)

        val afterUpdate = repo.findBySentStatus(false, 1)[0]

        assertThat(afterUpdate?.read).isEqualTo(true)
    }

    @Test
    internal fun `Kan hente sendte men uleste`() {
        repo.updateSentStatus(dbVarsling.uuid, LocalDateTime.now(), true)

        val unread = repo.findSentButUnread(100)

        assertThat(unread).hasSize(1)
    }

    @AfterEach
    internal fun tearDown() {
        repo.remove(dbVarsling.uuid)
        stopKoin()
    }
}