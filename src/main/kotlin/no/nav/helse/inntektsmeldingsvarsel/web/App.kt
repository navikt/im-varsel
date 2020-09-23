package no.nav.helse.inntektsmeldingsvarsel.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.getAllOfType
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.selectModuleBasedOnProfile
import no.nav.helse.inntektsmeldingsvarsel.nais.nais
import no.nav.helse.inntektsmeldingsvarsel.varsling.SendVarslingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.UpdateReadStatusJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingProcessor
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory


@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
                .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->
        app.start(wait = false)

        val koin = app.application.getKoin()

        val manglendeInntektsmeldingMottak = koin.get<VarslingsmeldingProcessor>()
        manglendeInntektsmeldingMottak.startAsync(retryOnFail = true)

        val varslingSenderJob = koin.get<SendVarslingJob>()
        varslingSenderJob.startAsync(retryOnFail = true)

        val updateReadStatusJob = koin.get<UpdateReadStatusJob>()
        updateReadStatusJob.startAsync(retryOnFail = true)

        //autoDetectProbableComponents(koin)

        Runtime.getRuntime().addShutdownHook(Thread {
            varslingSenderJob.stop()
            manglendeInntektsmeldingMottak.stop()
            updateReadStatusJob.stop()
            app.stop(1000, 1000)
        })
    }
}

private fun autoDetectProbableComponents(koin: org.koin.core.Koin) {
    val kubernetesProbeManager = koin.get<KubernetesProbeManager>()

    koin.getAllOfType<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

    koin.getAllOfType<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }
}

@KtorExperimentalAPI
fun createApplicationEnvironment() = applicationEngineEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())

    connector {
        port = 8080
    }

    module {
        install(CallLogging) {
            level = org.slf4j.event.Level.INFO
        }

        install(Koin) {
            modules(selectModuleBasedOnProfile(config))
        }

        install(ContentNegotiation) {
            val commonObjectMapper = get<ObjectMapper>()
            register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
        }

        nais()
    }
}

