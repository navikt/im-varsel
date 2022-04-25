package no.nav.helse.slowtests.systemtest.api

import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class AltInnBrevRoutesHTTPTests : SystemTestBase() {
    private val altInnBrevRoutesUrl = "/brevmal"

    @Test
    fun `Skal returnere liste av altinn brevmal`() = suspendableTest {
        val response = httpClient.get<Set<AltinnOrganisasjon>> {
            appUrl(altInnBrevRoutesUrl)
            contentType(ContentType.Application.Json)
        }
        Assertions.assertThat(response.size).isGreaterThan(0)
    }
}
