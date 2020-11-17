package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.common
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.CreateTopicsOptions
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.KoinApplication
import org.koin.core.KoinComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


/**
 * Disse testene krever en kjørende Kafka broker på localhost:9092
 * For å kjøre opp en kan du gjøre
 * cd docker/local
 * docker-compose build
 * docker-compose up
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VarslingsmeldingKafkaClientClientTest : KoinComponent {
    private lateinit var adminClient: AdminClient
    val topicName = "manglende-inntektsmelding-test"
    lateinit var koin: KoinApplication

    val testProps = mutableMapOf<String, Any>(
            "bootstrap.servers" to "localhost:9092",
            "max.poll.interval.ms" to "30000"
    )

    @BeforeAll
    internal fun setUp() {
        koin = KoinApplication.create().modules(common)

        adminClient = KafkaAdminClient.create(testProps)

        adminClient
                .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                .all()
                .get(20, TimeUnit.SECONDS)
    }

    @AfterAll
    internal fun tearDown() {
        adminClient.deleteTopics(mutableListOf(topicName))
        adminClient.close()
    }

    @Test
    internal fun testHealthCheck() {
        val client = VarslingsmeldingKafkaClient(testProps, topicName)

        runBlocking { client.runLivenessCheck() }

        client.stop()

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.getMessagesToProcess() }
        }

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.runLivenessCheck() }
        }
    }

    @Test
    fun getMessages() {

        val client = VarslingsmeldingKafkaClient(testProps, topicName)
        val noMessagesExpected = client.getMessagesToProcess()

        assertThat(noMessagesExpected).isEmpty()

        val producer = KafkaProducer<String, String>(testProps, StringSerializer(), StringSerializer())
        val om = koin.koin.get<ObjectMapper>()

        producer.send(
                ProducerRecord(topicName, om.writeValueAsString(SpleisInntektsmeldingMelding(
                        "222323",
                        LocalDate.now(),
                        LocalDate.now().plusDays(7),
                        LocalDateTime.now(),
                        "0102030405718"
                )))
        ).get(10, TimeUnit.SECONDS)

        val oneMessageExpected = client.getMessagesToProcess()
        assertThat(oneMessageExpected).hasSize(1)

        val stillSameMEssageExpected = client.getMessagesToProcess()
        assertThat(stillSameMEssageExpected).hasSize(1)
        assertThat(oneMessageExpected.first()).isEqualTo(stillSameMEssageExpected.first())

        client.confirmProcessingDone()

        val zeroMessagesExpected = client.getMessagesToProcess()
        assertThat(zeroMessagesExpected).isEmpty()

        client.stop()
    }
}