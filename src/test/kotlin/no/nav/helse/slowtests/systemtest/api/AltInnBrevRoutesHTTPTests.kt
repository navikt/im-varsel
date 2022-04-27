package no.nav.helse.slowtests.systemtest.api

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.TestData
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevmal
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import java.util.*
import javax.sql.DataSource

class AltInnBrevRoutesHTTPTests : SystemTestBase() {
    private val altInnBrevRoutesUrl = "/brevmal"

    lateinit var repo: PostgresAltinnBrevmalRepository
    lateinit var dataSource: DataSource

    private val brevmal = TestData.AltinnBrevmal

    @BeforeEach
    internal fun setUp() {
        dataSource = get<DataSource>()
        repo = PostgresAltinnBrevmalRepository(dataSource, get())
    }

    @Test
    fun `Skal returnere liste av altinn brevmal`() = suspendableTest {
        repo.insert(brevmal)
        val response = httpClient.get< Set<AltinnBrevmal>> {
            appUrl(altInnBrevRoutesUrl)
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.size).isGreaterThan(0)
    }
    @Test
    fun `Skal en bestemt altinn brevmal`() = suspendableTest {
        repo.insert(brevmal)
        val response = httpClient.get<AltinnBrevmal> {
            appUrl(altInnBrevRoutesUrl + "/" + brevmal.id.toString())
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.id).isEqualTo(brevmal.id)
    }

    // Todo: sjekker egentlig bare at vi får et bytearray for er større enn 0, ikke at vi mottar en PDF
    @Test
    fun `Skal en bestemt altinn brevmal i PDF`() = suspendableTest {
        repo.insert(brevmal)
        val response = httpClient.get<ByteArray> {
            appUrl(altInnBrevRoutesUrl + "/" + brevmal.id.toString() + ".pdf")
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.size).isGreaterThan(0)
    }

    @Test
    fun `Skal kunne oppdatere en mal`() = suspendableTest {
        val brevmalOppdatert = brevmal.copy(summary = "Dette er oppdatert")
        repo.insert(brevmal)
        val repoSize = repo.getAll().size
        val response = httpClient.put<AltinnBrevmal> {
            appUrl(altInnBrevRoutesUrl)
            body = brevmalOppdatert
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.id).isEqualTo(brevmal.id)
        Assertions.assertThat(response.summary).isEqualTo("Dette er oppdatert")
        Assertions.assertThat(repo.getAll().size).isEqualTo(repoSize)
    }

    @Test
    fun `Skal kunne sette inn en mal`() = suspendableTest {
        val brevmalNy = brevmal.copy(id = UUID.randomUUID(), summary = "Dette er oppdatert")
        val repoSize = repo.getAll().size
        val response = httpClient.post<AltinnBrevmal> {
            appUrl(altInnBrevRoutesUrl)
            body = brevmalNy
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.id).isNotEqualTo(brevmal.id)
        Assertions.assertThat(response.summary).isEqualTo("Dette er oppdatert")
        Assertions.assertThat(repo.getAll().size).isEqualTo(repoSize + 1)
    }
}
