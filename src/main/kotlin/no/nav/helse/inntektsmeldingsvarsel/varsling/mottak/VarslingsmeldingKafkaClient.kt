package no.nav.helse.inntektsmeldingsvarsel.varsling.mottak

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.inntektsmeldingsvarsel.ANTALL_INNKOMMENDE_MELDINGER
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration

interface ManglendeInntektsmeldingMeldingProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class VarslingsmeldingKafkaClient(props: MutableMap<String, Any>, topicName: String) :
    ManglendeInntektsmeldingMeldingProvider,
    LivenessComponent {
    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>

    private val log = LoggerFactory.getLogger(VarslingsmeldingKafkaClient::class.java)

    init {
        props.apply {
            put("enable.auto.commit", false)
            put("group.id", "helsearbeidsgiver-im-varsel")
            put("max.poll.interval.ms", Duration.ofMinutes(10).toMillis().toInt())
            put("auto.offset.reset", "earliest")
        }

        consumer = KafkaConsumer(props, StringDeserializer(), StringDeserializer())
        consumer.subscribe(listOf(topicName))

        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.debug("Got shutdown message, closing Kafka connection...")
                consumer.close()
                log.debug("Kafka connection closed")
            },
        )
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val kafkaMessages = consumer.poll(Duration.ofSeconds(10))
            val payloads = kafkaMessages.map { it.value() }
            lastThrown = null
            currentBatch = payloads

            log.debug("Fikk ${kafkaMessages.count()} meldinger med offsets ${kafkaMessages.map { it.offset() }.joinToString(", ")}")
            return payloads
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
        ANTALL_INNKOMMENDE_MELDINGER.inc(currentBatch.size.toDouble())
        currentBatch = emptyList()
    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}
