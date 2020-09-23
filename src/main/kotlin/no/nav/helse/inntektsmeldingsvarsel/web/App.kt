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
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.getAllOfType
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.selectModuleBasedOnProfile
import no.nav.helse.inntektsmeldingsvarsel.nais.nais
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory

val mainLogger = LoggerFactory.getLogger("main()")

@KtorExperimentalAPI
fun main() {


    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        mainLogger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { httpServer ->
        mainLogger.info("Starter opp KTOR")
        httpServer.start(wait = false)
        mainLogger.info("KTOR Startet")

        val koin = httpServer.application.getKoin()
        mainLogger.info("Koin Startet")

        /*
        val manglendeInntektsmeldingMottak = koin.get<VarslingsmeldingProcessor>()
        manglendeInntektsmeldingMottak.startAsync(retryOnFail = true)

        val varslingSenderJob = koin.get<SendVarslingJob>()
        varslingSenderJob.startAsync(retryOnFail = true)

        val updateReadStatusJob = koin.get<UpdateReadStatusJob>()
        updateReadStatusJob.startAsync(retryOnFail = true)
*/
        runBlocking { autoDetectProbableComponents(koin) }

        mainLogger.info("La til probable komponentner")

        Runtime.getRuntime().addShutdownHook(Thread {
            LoggerFactory.getLogger("shutdownHook").info("Received shutdown signal")

            /*
            varslingSenderJob.stop()
            manglendeInntektsmeldingMottak.stop()
            updateReadStatusJob.stop()


             */
            httpServer.stop(1000, 1000)
        })
    }
}

private suspend fun autoDetectProbableComponents(koin: org.koin.core.Koin) {
    val kubernetesProbeManager = koin.get<KubernetesProbeManager>()

    koin.getAllOfType<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

    mainLogger.info("La til liveness")

    koin.getAllOfType<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

    mainLogger.info("La til readyness")

    mainLogger.info("Helsesjekk: ${kubernetesProbeManager.runReadynessProbe()}")
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

