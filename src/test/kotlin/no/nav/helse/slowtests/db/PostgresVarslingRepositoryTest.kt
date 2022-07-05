package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingDbEntity
import no.nav.helse.slowtests.KoinTestBase
import no.nav.helse.slowtests.clearAllDatabaseTables
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PostgresVarslingRepositoryTest : KoinTestBase() {

    lateinit var repo: PostgresVarslingRepository
    lateinit var dataSource: HikariDataSource

    private val dbVarsling = VarslingDbEntity(
        uuid = UUID.randomUUID().toString(),
        data = "[]",
        sent = false,
        read = false,
        opprettet = LocalDateTime.now(),
        virksomhetsNr = "123456789",
        virksomhetsNavn = "Stark Industries"
    )

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresVarslingRepository(dataSource)
        repo.insert(dbVarsling, dataSource.connection)
    }

    @Test
    internal fun `kan inserte og hente`() {
        val fradb = repo.findBySentStatus(false, 1)[0]

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
        val timeOfUpdate = LocalDateTime.now()
        repo.updateReadStatus(dbVarsling.uuid, timeOfUpdate, true)

        val afterUpdate = repo.findBySentStatus(false, 1)[0]

        assertThat(afterUpdate?.read).isEqualTo(true)
        assertThat(afterUpdate?.lestTidspunkt).isEqualTo(timeOfUpdate)
    }

    @Test
    internal fun `ikke feil hvis lestTidspunkt null`() {
        val afterUpdate = repo.findBySentStatus(false, 1)[0]
        assertThat(afterUpdate?.read).isEqualTo(false)
        assertThat(afterUpdate?.lestTidspunkt).isNull()
    }

    @Test
    internal fun `Kan hente sendte men uleste`() {
        repo.updateSentStatus(dbVarsling.uuid, LocalDateTime.now(), true)

        val unread = repo.findSentButUnread(100)

        assertThat(unread).hasSize(1)
    }

    @Test
    internal fun `skal kunne oppdatere journalpostId via journalført`() {
        repo.updateJournalført(dbVarsling.uuid, "jp-123-789")
        val afterUpdate = repo.findBySentStatus(false, 1)[0]
        assertThat(afterUpdate?.journalpostId).isEqualTo("jp-123-666")
    }

    @AfterEach
    internal fun tearDown() {
        clearAllDatabaseTables()
    }
}
