package no.nav.helse.inntektsmeldingsvarsel.web

import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevmal
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.pdf.PDFGenerator
import org.koin.ktor.ext.get
import java.util.*


/**
 * Enkel UI og API ment for internt bruk.
 *
 * GUIet og APIet må skrus på via miljøvariabelen ALTINN_BREVUTSENDELSE_UI_ENABLED=true
 */
@KtorExperimentalAPI
fun Application.altinnBrevRoutes() {

    routing {
        static("ui") {
            resources("brevutsendelse")
            defaultResource("index.html", "brevutsendelse")
        }
    }

    routing {
        val brevMalRepository = get<AltinnBrevMalRepository>()

        get("/brevmal") {
            val mal = brevMalRepository.getAll()
            call.respond(mal)
        }


        get("/brevmal/{id}") {
            val uuid = UUID.fromString(call.parameters.get("id"))
            val mal = brevMalRepository.get(uuid)
            call.respond(mal)
        }

        get("/brevmal/{id}.pdf") {
            val uuid = UUID.fromString(call.parameters.get("id"))
            val mal = brevMalRepository.get(uuid)
            val pdf = PDFGenerator().lagPDF(mal)
            call.respondBytes(pdf)
        }

        put("/brevmal") {
            val mal = call.receive<AltinnBrevmal>()
            brevMalRepository.update(mal)
            call.respond(mal)
        }

        post("/brevmal") {
            val mal = call.receive<AltinnBrevmal>()
            brevMalRepository.insert(mal)
            call.respond(mal)
        }
    }

    routing {
        val brevutesendelseRepo = get<AltinnBrevUtsendelseRepository>()

        post("/opprett-utsendelse") {
            val request = call.receive<BrevutsendelseInsertRequest>()
            brevutesendelseRepo.insertUtsendelse(request.malId, request.virksomhetsnummere)
            call.respond("OK")
        }
    }
}

data class BrevutsendelseInsertRequest(val malId: UUID, val virksomhetsnummere: Set<String>)