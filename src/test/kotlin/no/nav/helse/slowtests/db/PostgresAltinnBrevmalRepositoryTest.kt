package no.nav.helse.slowtests.db

import no.nav.helse.TestData
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.slowtests.KoinTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import java.util.*
import javax.sql.DataSource

internal class PostgresAltinnBrevmalRepositoryTest : KoinTestBase() {

    lateinit var repo: PostgresAltinnBrevmalRepository
    lateinit var dataSource: DataSource

    private val brevmal = TestData.AltinnBrevmal

    @BeforeEach
    internal fun setUp() {
        dataSource = get<DataSource>()
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
    }
}
