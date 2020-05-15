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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread

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