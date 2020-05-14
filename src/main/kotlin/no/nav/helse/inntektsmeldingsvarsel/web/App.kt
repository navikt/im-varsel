package no.nav.helse.inntektsmeldingsvarsel.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.application.install
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.util.KtorExperimentalAPI
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.inntektsmeldingsvarsel.AltinnVarselSender
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.getString
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.selectModuleBasedOnProfile
import no.nav.helse.inntektsmeldingsvarsel.nais.nais
import no.nav.helse.inntektsmeldingsvarsel.varsling.SendVarslingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.UpdateReadStatusJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingProcessor
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import kotlin.concurrent.thread


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

        sendMessages(app)

        Runtime.getRuntime().addShutdownHook(Thread {
            varslingSenderJob.stop()
            manglendeInntektsmeldingMottak.stop()
            updateReadStatusJob.stop()
            app.stop(1000, 1000)
        })
    }
}

fun sendMessages(app: NettyApplicationEngine) {
    val environment = app.environment
    if (environment.config.property("koin.profile").getString() != "PROD") {
        return
    }
    val log = LoggerFactory.getLogger("send-melding")
    val altinnClient = app.application.get<ICorrespondenceAgencyExternalBasic>()

    val virksomheter = when {
        environment.config.property("koin.profile").getString() == "PROD" -> AltinnVarselSender::class.java.getResource("/virksomheter").readText().split("\n")
        else -> AltinnVarselSender::class.java.getResource("/virksomheter_test").readText().split("\n")
    }


    val serviceCode = environment.config.getString("altinn_melding.service_id")
    val username = environment.config.getString("altinn_melding.username")
    val password = environment.config.getString("altinn_melding.password")

    val filtrert = virksomheter.map { it.trim() }.filter { it.isNotBlank() }
    log.info("Filtrert virksomhetsliste: ${filtrert.joinToString()}")

    thread {
        filtrert.forEach {
            sleep(2000)
            log.info("Sender for $it")

            try {
                val receiptExternal = altinnClient.insertCorrespondenceBasicV2(
                        username, password,
                        AltinnVarselSender.SYSTEM_USER_CODE, "nav-im-melding-korona-$it",
                        createMelding(serviceCode, it)
                )

                log.info("Respons fra Altinn: ${receiptExternal.receiptStatusCode}")

                if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                    throw RuntimeException("Fikk uventet statuskode fra Altinn: ${receiptExternal.receiptStatusCode}")
                }
                log.info("Sendt OK $it")

            } catch (ex: Exception) {
                log.error("$it feilet", ex)
            }
        }
    }
}

fun createMelding(altinnTjenesteKode: String, virksomhetsNr: String): InsertCorrespondenceV2 {
    val tittel = "Om utbetaling av sykepenger i Covid19-tilfeller"

    val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>Om utbetaling av sykepenger i Covid19-tilfeller</h2>
                       <p>
                        Du får denne meldingen fordi du har oppgitt i en inntektsmelding at du ikke utbetaler sykepenger i arbeidsgiverperioden etter 12. mars 2020. <br>
                        Vi minner om at arbeidsgivere fortsatt må utbetale sykepenger i 16 dager, også ved Covid19-tilfeller. 
                        </p>
                        <p>
                        Det nye er at dere i slike tilfeller kan kreve refusjon fra dag 4 i ettertid. <br>
                        Kravskjemaet finner du på Min side - arbeidsgiver på <a href="https://nav.no">nav.no</a>.
                        </p>
                   </div>
               </body>
            </html>
        """.trimIndent()

    val meldingsInnhold = ExternalContentV2()
            .withLanguageCode("1044")
            .withMessageTitle(tittel)
            .withMessageBody(innhold)
            .withMessageSummary("Om utbetaling av sykepenger i Covid19-tilfeller")

    return InsertCorrespondenceV2()
            .withAllowForwarding(false)
            .withReportee(virksomhetsNr)
            .withMessageSender("NAV (Arbeids- og velferdsetaten)")
            .withServiceCode(altinnTjenesteKode)
            .withServiceEdition("1")
            .withContent(meldingsInnhold)
}


@KtorExperimentalAPI
fun createApplicationEnvironment() = applicationEngineEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())

    connector {
        port = 8080
    }

    module {
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

