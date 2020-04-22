package no.nav.helse.inntektsmeldingsvarsel.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.coroutines.runBlocking
import no.nav.helse.inntektsmeldingsvarsel.RestStsClient
import org.apache.cxf.ws.security.tokenstore.SecurityToken
import org.slf4j.LoggerFactory

class PdlClient(
        private val pdlUrl: String,
        private val stsClient: RestStsClient,
        private val httpClient: HttpClient
) {
    fun person(ident: String): PdlHentPerson? {
        val stsToken = stsClient.getOidcToken()
        val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace("[\n\r]", "")
        val entity = PdlRequest(query, Variables(ident))
        try {
            val pdlPersonReponse = runBlocking {
                 httpClient.post<PdlPersonResponse> {
                    url(pdlUrl)
                    body = entity
                    header("Tema", "SYK")
                    header("Authorization", "Bearer $stsToken")
                    header("Nav-Consumer-Token", "Bearer $stsToken")
                }
            }

            return if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
                pdlPersonReponse.errors.forEach {
                    LOG.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                null
            } else {
                pdlPersonReponse.data
            }
        } catch (exception: Exception) {
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    fun personName(ident: String): String? {
        return person(ident)?.fullName()
    }

    fun isKode6Or7(ident: String): Boolean {
        return person(ident)?.isKode6Or7() ?: true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlClient::class.java)
    }
}