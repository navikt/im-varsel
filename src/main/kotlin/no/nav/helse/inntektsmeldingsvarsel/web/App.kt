package no.nav.helse.inntektsmeldingsvarsel.web

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.SendAltinnBrevUtsendelseJob
import no.nav.helse.inntektsmeldingsvarsel.datapakke.DatapakkePublisherJob
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.selectModuleBasedOnProfile
import no.nav.helse.inntektsmeldingsvarsel.varsling.SendVarslingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.UpdateReadStatusJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.PollForVarslingsmeldingJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.slf4j.LoggerFactory

val mainLogger = LoggerFactory.getLogger("main()")

class ImVarselApplication(val port: Int = 8080) : KoinComponent {
    private val logger = LoggerFactory.getLogger(ImVarselApplication::class.simpleName)
    private var webserver: NettyApplicationEngine? = null
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())
    private val runtimeEnvironment = appConfig.getEnvironment()

    fun start() {
        if (runtimeEnvironment == AppEnv.PREPROD || runtimeEnvironment == AppEnv.PROD) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        // migrateDatabase()

        configAndStartBackgroundWorker()
        autoDetectProbeableComponents()
        configAndStartWebserver()
    }

    private fun autoDetectProbeableComponents() {
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAll<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

        getKoin().getAll<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

        logger.debug("La til probeable komponenter")
    }

    fun shutdown() {
        webserver?.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        stopKoin()
    }

    private fun configAndStartWebserver() {
        webserver = embeddedServer(
            Netty,
            applicationEngineEnvironment {
                config = appConfig
                connector {
                    port = this@ImVarselApplication.port
                }

                module {
                    if (runtimeEnvironment != AppEnv.PROD) {
                        // localCookieDispenser(config)
                    }

                    imVarselModule(config)
                }
            }
        )
        mainLogger.info("Starter opp KTOR")
        webserver!!.start(wait = false)
        mainLogger.info("KTOR Startet")
    }

    private fun configAndStartBackgroundWorker() {
        if (appConfig.getString("run_background_workers") == "true") {
            get<DatapakkePublisherJob>().startAsync(true)
            get<PollForVarslingsmeldingJob>().startAsync(retryOnFail = true)
            get<SendVarslingJob>().startAsync(retryOnFail = true)
            get<UpdateReadStatusJob>().startAsync(retryOnFail = true)
            get<SendAltinnBrevUtsendelseJob>().startAsync(retryOnFail = true)
            get<DatapakkePublisherJob>().startAsync(retryOnFail = true)
        }
    }
}

fun main() {
    val logger = LoggerFactory.getLogger("main")

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = ImVarselApplication()
    application.start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Fikk shutdown-signal, avslutter...")
            application.shutdown()
            logger.info("Avsluttet OK")
        }
    )
}
