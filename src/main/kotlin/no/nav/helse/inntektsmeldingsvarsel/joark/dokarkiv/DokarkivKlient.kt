package no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.readText
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.inntektsmeldingsvarsel.RestStsClient
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsVarselDbEntity
import no.nav.helse.sporenstreks.integrasjon.rest.dokarkiv.JournalpostResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface DokarkivKlient {
    fun journalførDokument(dokument: String, varsel: Varsling, callId: String): String
}

class MockDokarkivKlient : DokarkivKlient {
    override fun journalførDokument(dokument: String, varsel: Varsling, callId: String): String {
        return "id"
    }
}

class DokarkivKlientImpl(
        private val dokarkivBaseUrl: String,
        private val httpClient: HttpClient,
        private val stsClient: RestStsClient) : DokarkivKlient {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("DokarkivClient")


    override fun journalførDokument(dokument: String, varsel: Varsling, callId: String): String {
        try {
            logger.debug("Journalfører dokument");
            val url = "$dokarkivBaseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
            val response = runBlocking {
                httpClient.post<JournalpostResponse> {
                    url(url)
                    headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                    headers.append("Nav-Call-Id", callId)
                    contentType(io.ktor.http.ContentType.Application.Json)
                    body = JournalpostRequest(
                            bruker = Bruker(varsel.virksomhetsNr),
                            eksternReferanseId = varsel.uuid,
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
            assert(response.journalpostFerdigstilt)
            return response.journalpostId
        } catch (e: ClientRequestException) {
            runBlocking { logger.error("Feilet i journalføring med tilbakemelding: " + e.response.readText()) }
            throw e
        }
    }


    fun journalførDokument(dokument: String, varsel: PermisjonsVarselDbEntity, callId: String): String {
        try {
            logger.debug("Journalfører dokument");
            val url = "$dokarkivBaseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
            val response = runBlocking {
                httpClient.post<JournalpostResponse> {
                    url(url)
                    headers.append("Authorization", "Bearer " + stsClient.getOidcToken())
                    headers.append("Nav-Call-Id", callId)
                    contentType(io.ktor.http.ContentType.Application.Json)
                    body = JournalpostRequest(
                            bruker = Bruker(varsel.virksomhetsNr),
                            eksternReferanseId = "covidpermaltinn-${varsel.id}",
                            avsenderMottaker = AvsenderMottaker(
                                    id = varsel.virksomhetsNr
                            ),
                            dokumenter = listOf(Dokument(
                                    dokumentVarianter = listOf(DokumentVariant(
                                            fysiskDokument = dokument
                                    ))
                            )),
                            datoMottatt = LocalDate.now()
                    )
                }
            }
            assert(response.journalpostFerdigstilt)
            return response.journalpostId
        } catch (e: ClientRequestException) {
            runBlocking { logger.error("Feilet i journalføring med tilbakemelding: " + e.response.readText()) }
            throw e
        }
    }

}

