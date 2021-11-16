package no.nav.helse.slowtests.datapakke

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.datapakke.DatapakkePublisherJob
import no.nav.helse.inntektsmeldingsvarsel.db.IStatsRepo
import no.nav.helse.inntektsmeldingsvarsel.db.VarselStats
import no.nav.helse.slowtests.KoinTestBase
import no.nav.helse.slowtests.kafka.KafkaProducerForTests
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koin.core.inject
import java.util.*
import kotlin.random.Random

class DatapakkePublisherJobTest : KoinTestBase(){
    val httpClient = mockk<HttpClient>()
    val repo = mockk<IStatsRepo>()

    @BeforeAll
    internal fun setUp() {
        every { repo.getVarselStats() } returns (1..25)
            .map {
                VarselStats(
                    it,
                    Random.nextInt(1000),
                    Random.nextInt(100)
                )
            }.toList()
    }

    @Test
    @Disabled
    internal fun name() {
        val om = ObjectMapper()
        om.registerModule(KotlinModule())
        om.registerModule(Jdk8Module())
        om.registerModule(JavaTimeModule())
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        om.configure(SerializationFeature.INDENT_OUTPUT, true)
        om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        })

        DatapakkePublisherJob(
            repo,
            httpClient,
            "https://datakatalog-api.dev.intern.nav.no/v1/datapackage",
            "5683d0148392e99e79737fe6889aae68",
            om = om
        ).doJob()

    }
}
