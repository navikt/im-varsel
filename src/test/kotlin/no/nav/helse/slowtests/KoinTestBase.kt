package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.config.*
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.common
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.localDevConfig
import no.nav.helse.slowtests.kafka.KafkaProducerForTests
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get

/**
 * Disse testene krever en kjørende Kafka broker på localhost:9092
 * For å kjøre opp en kan du gjøre
 * cd docker/local
 * docker-compose build
 * docker-compose up
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class KoinTestBase : KoinComponent {

    @BeforeAll
    fun initKoinContext() {
        clearAllDatabaseTables()

        val runLocalModule = localDevConfig(MapApplicationConfig(
                "altinn_melding.kafka_topic" to KafkaProducerForTests.topicName
        ))

        startKoin {
            modules(listOf(common, runLocalModule))
        }
        println("Koin init")
    }

    @AfterAll
    fun stopKoinContext() {
        println("Koin stop")
        stopKoin()
    }
}

class TestKoinBaseTest: KoinTestBase() {
    @BeforeAll
    fun init() {
        println("test before")
    }

    @Test
    internal fun name() {
        val om = get<ObjectMapper>()
        println("test")
    }

    @AfterAll
    fun after() {
        println("test after")
    }
}