package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.TestData
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevmal
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.util.*

internal class PostgresAltinnBrevmalRepositoryTest : KoinComponent {

    lateinit var repo: PostgresAltinnBrevmalRepository
    lateinit var dataSource: HikariDataSource

    private val brevmal = TestData.AltinnBrevmal

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresAltinnBrevmalRepository(dataSource, get())
    }

    @Test
    internal fun `kan inserte og hente via ID`() {
        repo.insert(brevmal)
        val fradb = repo.get(brevmal.id)
        assertThat(fradb).isEqualTo(brevmal)
    }

    @Test
    internal fun `kan oppdatere brevmal`() {
        repo.insert(brevmal)

        val endretBrevmal = brevmal.copy(
                header = "Ny header",
                altinnTjenestekode = "ny kode",
                altinnTjenesteVersjon = "ny tjenesteversjon",
                bodyHtml = "ny melding",
                joarkBrevkode = "ny brevkode",
                joarkTema = "nytt tema",
                joarkTittel = "ny tittel",
                summary = "ny oppsummering"
        )

        repo.update(endretBrevmal)

        val endretFraDb = repo.get(brevmal.id)

        assertThat(endretFraDb).isEqualTo(endretBrevmal)
    }

    @Test
    internal fun `kan hente alle brevmaler`() {
        val antall = 5
        (1..antall).forEach {
            repo.insert(brevmal.copy(id = UUID.randomUUID()))
        }

        val alleBrevmaler = repo.getAll()

        assertThat(alleBrevmaler).hasSize(antall)
        alleBrevmaler.forEach { repo.delete(it.id) }
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(brevmal.id)
        stopKoin()
    }
}