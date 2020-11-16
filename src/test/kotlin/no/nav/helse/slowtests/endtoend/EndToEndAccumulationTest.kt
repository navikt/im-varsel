package no.nav.helse.slowtests.endtoend

import io.ktor.config.*
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.common
import no.nav.helse.inntektsmeldingsvarsel.dependencyinjection.localDevConfig
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.SendVarslingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.UpdateReadStatusJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.VarslingService
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.PollForVarslingsmeldingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.SpleisInntektsmeldingMelding.Meldingstype.TRENGER_IKKE_INNTEKTSMELDING
import no.nav.helse.slowtests.kafka.KafkaProducerForTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.KoinApplication
import org.koin.core.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.get
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndToEndAccumulationTest : KoinComponent {
    lateinit var kafkaProdusent: KafkaProducerForTests

    val manglerImMelding = SpleisInntektsmeldingMelding(
        "123456785",
        LocalDate.now(),
        LocalDate.now().plusDays(7),
        LocalDateTime.now().minusDays(VarslingService.VENTETID_I_DAGER),
        "01234567890"
    )

    @BeforeAll
    internal fun setUp() {
        val runLocalModule = localDevConfig(MapApplicationConfig(
                "altinn_melding.kafka_topic" to KafkaProducerForTests.topicName
        ))

        GlobalContext.start(KoinApplication.create().modules(listOf(common, runLocalModule)))
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.tearDown()
    }

    @Test
    internal fun `Fjerner ventende ved beskjed og sender for ventende eldre enn grensen`() {
        val sendJob = getKoin().get<SendVarslingJob>()
        val mottaksJob = getKoin().get<PollForVarslingsmeldingJob>()
        val readReceiptJob = getKoin().get<UpdateReadStatusJob>()

        // send meldinger på kafka: samme person 2 arbeidsforhold der den ene
        kafkaProdusent = KafkaProducerForTests(get())
        kafkaProdusent.sendSync(manglerImMelding)
        kafkaProdusent.sendSync(manglerImMelding.copy(organisasjonsnummer = "123"))
        kafkaProdusent.sendSync(manglerImMelding.copy(meldingsType = TRENGER_IKKE_INNTEKTSMELDING))

        mottaksJob.doJob()

        // assert at det er riktig akkumulert i databasen
        val ventendeRepo = getKoin().get<VentendeBehandlingerRepository>()
        val ventendeMeldinger = ventendeRepo.findOlderThan(LocalDateTime.now())
        assertThat(ventendeMeldinger).hasSize(1)

        sendJob.doJob()

        // assert at ting er riktig merket i databasen
        val varslingRepo = getKoin().get<VarslingRepository>()
        val sentVarslinger = varslingRepo.findSentButUnread(10)
        assertThat(sentVarslinger).hasSize(1)

        readReceiptJob.doJob()

        // assert at ting er riktig merket i databasen
        val unreadVarslinger = varslingRepo.findSentButUnread(1)
        assertThat(unreadVarslinger).hasSize(0)
    }
}