package no.nav.helse.inntektsmeldingsvarsel.nais

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.inntektsmeldingsvarsel.AltinnVarselSender
import no.nav.helse.inntektsmeldingsvarsel.selfcheck.HealthCheck
import no.nav.helse.inntektsmeldingsvarsel.selfcheck.HealthCheckState
import no.nav.helse.inntektsmeldingsvarsel.selfcheck.HealthCheckType
import no.nav.helse.inntektsmeldingsvarsel.selfcheck.runHealthChecks
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsMeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.getAllOfType
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.getString
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val collectorRegistry = CollectorRegistry.defaultRegistry

@KtorExperimentalAPI
fun Application.nais() {

    DefaultExports.initialize()

    routing {
        get("/isalive") {
            returnResultOfChecks(this@routing, HealthCheckType.ALIVENESS, this)
        }

        get("/isready") {
            returnResultOfChecks(this@routing, HealthCheckType.READYNESS, this)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val allRegisteredSelfCheckComponents = this@routing.getKoin().getAllOfType<HealthCheck>()
            val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
            val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK

            call.respond(httpResult, checkResults)
        }

        get("/send-meldinger-til-virksomheter-med-feil-innsendinger") {
            val log = LoggerFactory.getLogger("/send-meldinger-til-virksomheter-med-feil-innsendinger")
            val altinnClient = this@routing.get<ICorrespondenceAgencyExternalBasic>()

            val virksomheter = when {
                environment.config.property("koin.profile").getString() == "PROD" -> AltinnVarselSender::class.java.getResource("/virksomheter").readText().split("\n")
                else -> AltinnVarselSender::class.java.getResource("/virksomheter_test").readText().split("\n")
            }

            val serviceCode = environment.config.getString("altinn_melding.service_id")
            val username = environment.config.getString("altinn_melding.username")
            val password = environment.config.getString("altinn_melding.password")

            virksomheter.map { it.trim() }.filter { it.isNotBlank() }.forEach {
                log.info("Sender for $it")

                try {
                    val receiptExternal = altinnClient.insertCorrespondenceBasicV2(
                            username, password,
                            AltinnVarselSender.SYSTEM_USER_CODE, "nav-im-melding-korona-$it",
                            createMelding(serviceCode, it)
                    )
                    if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                        throw RuntimeException("Fikk uventet statuskode fra Altinn: ${receiptExternal.receiptStatusCode}")
                    }
                    log.info("Sendt OK $it")

                } catch (ex: Exception) {
                    log.error("$it feilet", ex)
                }
            }

            call.respond(HttpStatusCode.OK, "OK")
        }

        get("/send-altinn-melding") {

            val log = LoggerFactory.getLogger("/send-altinn-melding")

            if (environment.config.property("koin.profile").getString() == "PROD") {
                call.respond(HttpStatusCode.ExpectationFailed, "Kan ikke kalles i PROD")
                return@get
            }

            val cfg = this@routing.application.environment.config

            val topicName = cfg.getString("altinn_melding.kafka_topic")
            log.info("Sender melding på topic $topicName")
            val producer = KafkaProducer<String, String>(mutableMapOf<String, Any>(
                    "bootstrap.servers" to cfg.getString("kafka.endpoint"),
                    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
                    SaslConfigs.SASL_MECHANISM to "PLAIN",
                    SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                            "username=\"${cfg.getString("kafka.username")}\" password=\"${cfg.getString("kafka.password")}\";"
            ), StringSerializer(), StringSerializer())

            val om = this@routing.get<ObjectMapper>()

            val messageString = om.writeValueAsString(ManglendeInntektsMeldingMelding(
                    "810007842", //  -> Anstendig Piggsvin Barnehage
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(7),
                    LocalDateTime.now().minusWeeks(2),
                    "09088723349"
            ))

            log.info("Sender melding $messageString")

            producer.send(ProducerRecord(topicName, messageString))

            call.respond(HttpStatusCode.OK, "Melding sendt til Kø: \n$messageString")
        }
    }
}

private suspend fun returnResultOfChecks(routing: Routing, type: HealthCheckType, pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val allRegisteredSelfCheckComponents = routing.getKoin()
            .getAllOfType<HealthCheck>()
            .filter { it.healthCheckType == type }

    val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
    val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK
    checkResults.forEach { r ->
        r.error?.let { pipelineContext.call.application.environment.log.error(r.toString()) }
    }
    pipelineContext.call.respond(httpResult, checkResults)
}


fun createMelding(altinnTjenesteKode: String, virksomhetsNr: String): InsertCorrespondenceV2 {
    val tittel = "Ang utbetaling av sykepenger ifbm Covid19-tilfeller"

    val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>Angående utbetaling av sykepenger ifbm Covid19-tilfeller</h2>
                       <p>
                        Du får denne meldingen fordi du har oppgitt i en inntektsmelding at du ikke betaler sykepenger i arbeidsgiverperioden etter 12. mars 2020. <br>
                        Vi minner om at arbeidsgivere fortsatt må utbetale sykepenger i 16 dager, også ved Covid19-tilfeller. 
                        </p>
                        <p>
                        Det nye er at dere i slike tilfeller kan kreve refusjon fra dag 4 i ettertid. <br>
                        Kravskjemaet finner du på Min side - arbeidsgiver på <a href="nav.no">nav.no</a>.
                        </p>
                   </div>
               </body>
            </html>
        """.trimIndent()

    val meldingsInnhold = ExternalContentV2()
            .withLanguageCode("1044")
            .withMessageTitle(tittel)
            .withMessageBody(innhold)
            .withMessageSummary("Ang utbetaling av sykepenger ifbm Covid19-tilfeller")

    return InsertCorrespondenceV2()
            .withAllowForwarding(false)
            .withReportee(virksomhetsNr)
            .withMessageSender("NAV (Arbeids- og velferdsetaten)")
            .withServiceCode(altinnTjenesteKode)
            .withServiceEdition("1")
            .withContent(meldingsInnhold)
}
