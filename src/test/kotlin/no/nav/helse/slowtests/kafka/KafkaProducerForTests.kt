package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit

class KafkaProducerForTests(private val om: ObjectMapper) {

    val testProps = mutableMapOf<String, Any>(
            "bootstrap.servers" to "localhost:9092",
            "max.poll.interval.ms" to "30000"
    )

    val adminClient = KafkaAdminClient.create(testProps)
    val producer = KafkaProducer<String, String>(testProps, StringSerializer(), StringSerializer())

    init {
        adminClient
                .createTopics(mutableListOf(NewTopic(Companion.topicName, 1, 1)))
                .all()
                .get(20, TimeUnit.SECONDS)
    }

    fun sendSync(spleisMelding: SpleisInntektsmeldingMelding) {
        producer.send(
                ProducerRecord(Companion.topicName, om.writeValueAsString(spleisMelding))
        ).get(10, TimeUnit.SECONDS)
    }

    fun tearDown() {
        adminClient.deleteTopics(mutableListOf(Companion.topicName))
        adminClient.close()
    }

    companion object {
        val topicName = "manglende-inntektsmelding-test"
    }
}