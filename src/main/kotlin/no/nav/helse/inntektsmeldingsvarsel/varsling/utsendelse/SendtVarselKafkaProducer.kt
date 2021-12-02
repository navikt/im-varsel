package no.nav.helse.inntektsmeldingsvarsel.varsling.utsendelse

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class SendtVarselKafkaProducer(producerProperties: Map<String, Any>, private val topic: String, private val om: ObjectMapper) {
    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggSendtVarselPåTopic(inntektsmelding: SpleisInntektsmeldingMelding) {
        kafkaproducer.send(
            ProducerRecord(
                topic,
                inntektsmelding.organisasjonsnummer,
                serialiseringInntektsmelding(inntektsmelding)
            )
        )
    }

    fun serialiseringInntektsmelding(inntektsmelding: SpleisInntektsmeldingMelding) =
        om.writeValueAsString(inntektsmelding)
}
