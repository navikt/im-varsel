package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit

class KafkaProducerForTests(private val om: ObjectMapper) {

    val adminClient = KafkaAdminClient.create(Companion.testProps)
    val producer = KafkaProducer<String, String>(Companion.testProps, StringSerializer(), StringSerializer())

    init {
        try {
            adminClient
                    .deleteTopics(mutableListOf(topicName))
                    .all()
                    .get(20, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            print("test")
        }

        try {

        adminClient
                .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                .all()
                .get(20, TimeUnit.SECONDS)
        } catch(existsEx: TopicExistsException) {

        }
    }

    fun sendSync(spleisMelding: SpleisInntektsmeldingMelding) {
        producer.send(
                ProducerRecord(topicName, om.writeValueAsString(spleisMelding))
        ).get(10, TimeUnit.SECONDS)
    }

    fun tearDown() {
        adminClient.deleteTopics(mutableListOf(topicName))
        adminClient.close()
    }

    companion object {
        val topicName = "manglende-inntektsmelding-test"
        val testProps = mutableMapOf<String, Any>(
                "bootstrap.servers" to "localhost:9092",
                "max.poll.interval.ms" to "30000"
        )
    }
}