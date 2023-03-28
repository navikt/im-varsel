package no.nav.helse.slowtests.kafka

import kotlinx.coroutines.runBlocking
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import no.nav.helse.slowtests.KoinTestBase
import no.nav.helse.slowtests.kafka.KafkaProducerForTests.Companion.testProps
import no.nav.helse.slowtests.kafka.KafkaProducerForTests.Companion.topicName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.get
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingsmeldingKafkaClientClientTest : KoinTestBase() {
    lateinit var kafkaProdusent: KafkaProducerForTests

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaProducerForTests(get())
        kafkaProdusent.createTopicIfNotExists()
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
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

        kafkaProdusent.sendSync(
            SpleisInntektsmeldingMelding(
                "222323",
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                LocalDateTime.now(),
                "0102030405718",
            ),
        )
        Thread.sleep(100)
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
