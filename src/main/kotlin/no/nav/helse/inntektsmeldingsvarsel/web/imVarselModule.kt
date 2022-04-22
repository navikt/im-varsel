package no.nav.helse.inntektsmeldingsvarsel.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.IgnoreTrailingSlash
import no.nav.helse.arbeidsgiver.system.getString

import org.koin.ktor.ext.get

fun Application.imVarselModule(config: ApplicationConfig = environment.config) {
    install(IgnoreTrailingSlash)

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    nais()
    if (config.getString("altinn_brevutsendelse.ui_enabled").equals("true")) {
        altinnBrevRoutes()
    }
}
