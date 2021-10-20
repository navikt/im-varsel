package no.nav.helse.inntektsmeldingsvarsel.dependencyinjection

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.inntektsmeldingsvarsel.PilotAllowList
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.MockAltinnBrevutsendelseSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.SendAltinnBrevUtsendelseJob
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.getDataSource
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.*
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsmeldingMeldingProvider
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.PollForVarslingsmeldingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun localDevConfig(config: ApplicationConfig) = module {
    single { getDataSource(createLocalHikariConfig(), "im-varsel", null) as DataSource }

    single {
        VarslingsmeldingKafkaClient(mutableMapOf<String, Any>(
            "bootstrap.servers" to "localhost:9092",
            "max.poll.interval.ms" to "30000")
            , config.getString("altinn_melding.kafka_topic")) as ManglendeInntektsmeldingMeldingProvider
    }

    single { VarslingMapper(get()) }

    single { object : AccessTokenProvider { override fun getToken(): String { return "fake token"} } } bind AccessTokenProvider::class
    single {MockReadReceiptProvider() as ReadReceiptProvider}

    single {
        object : PdlClient {
            override fun fullPerson(ident: String) =
                PdlHentFullPerson(
                    PdlHentFullPerson.PdlFullPersonliste(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList()
                    ),

                    PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),

                    PdlHentFullPerson.PdlGeografiskTilknytning(
                        PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                        null,
                        null,
                        "SWE"
                    )
                )

            override fun personNavn(ident: String) =
                PdlHentPersonNavn.PdlPersonNavneliste(
                    listOf(
                        PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                            "Ola",
                            "M",
                            "Avsender",
                            PdlPersonNavnMetadata("freg")
                        )
                    )
                )
        }

    } bind PdlClient::class

    single { PostgresVarslingRepository(get()) as VarslingRepository }
    single { PostgresVentendeBehandlingerRepository(get()) as VentendeBehandlingerRepository }
    single { VarslingService(get(), get(), get(), get(), get(), get(), PilotAllowList(setOf('1'))) }

    single { MockVarslingSender(get()) as VarslingSender }
    single { PollForVarslingsmeldingJob(get(), get()) }
    single { SendVarslingJob(get(), get()) }
    single { UpdateReadStatusJob(get(), get()) }

    single { PostgresAltinnBrevUtsendelseRepository(get()) as AltinnBrevUtsendelseRepository }
    single { PostgresAltinnBrevmalRepository(get(), get()) as AltinnBrevMalRepository }
    single { MockAltinnBrevutsendelseSender() as AltinnBrevutsendelseSender }
    single { SendAltinnBrevUtsendelseJob(get(), get()) }
}
