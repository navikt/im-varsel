package no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.RestStsClient
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.JournalpostResponse
import org.slf4j.LoggerFactory

interface DokarkivKlient {
    fun journalførDokument(dokument: String, varsel: Varsling, callId: String, brukerId: String, idType: String): String
}

class MockDokarkivKlient : DokarkivKlient {
    override fun journalførDokument(dokument: String, varsel: Varsling, callId: String, brukerId: String, idType: String): String {
        return "id"
    }
}

class DokarkivKlientImpl(
        private val dokarkivBaseUrl: String,
        private val httpClient: HttpClient,
        private val stsClient: RestStsClient) : DokarkivKlient {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("DokarkivClient")


    override fun journalførDokument(dokument: String, varsel: Varsling, callId: String, brukerId: String, idType: String): String {
        try {
            logger.debug("Journalfører dokument");
            val url = "$dokarkivBaseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
            var eksternrefId = varsel.uuid
            if (idType == "FNR") {
                eksternrefId += "-" + varsel.liste.indexOfFirst { it.personnumer == brukerId }
            }

            val response = runBlocking {
                httpClient.post<JournalpostResponse> {
                    url(url)
                    headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                    headers.append("Nav-Call-Id", callId)
                    contentType(io.ktor.http.ContentType.Application.Json)
                    body = JournalpostRequest(
                            bruker = Bruker(brukerId, idType),
                            eksternReferanseId = eksternrefId,
                            avsenderMottaker = AvsenderMottaker(
                                    id = varsel.virksomhetsNr
                            ),
                            dokumenter = listOf(Dokument(
                                    dokumentVarianter = listOf(DokumentVariant(
                                            fysiskDokument = dokument
                                    ))
                            )),
                            datoMottatt = varsel.opprettet.toLocalDate()
                    )
                }
            }

            logger.info(response.toString())

            if(!response.journalpostFerdigstilt) {
                throw IllegalStateException("Kunne ikke ferdigstille $response")
            }
            return response.journalpostId
        } catch (e: ClientRequestException) {
            runBlocking { logger.error("Feilet i journalføring med tilbakemelding: " + (e.response?.readText() ?: "ukjent")) }
            throw e
        }
    }
}

