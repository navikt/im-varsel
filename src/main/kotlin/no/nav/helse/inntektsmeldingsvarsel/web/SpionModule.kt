package no.nav.helse.inntektsmeldingsvarsel.web

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.selectModuleBasedOnProfile
import no.nav.helse.inntektsmeldingsvarsel.nais.nais
import org.koin.ktor.ext.Koin


@KtorExperimentalAPI
fun Application.spionModule(config: ApplicationConfig = environment.config) {
    install(Koin) {
        modules(selectModuleBasedOnProfile(config))
    }

    nais()
}