package no.nav.helse.inntektsmeldingsvarsel.dependencyinjection

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.RestSTSAccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.inntektsmeldingsvarsel.*
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSenderImpl
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.SendAltinnBrevUtsendelseJob
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.inntektsmeldingsvarsel.db.createHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.getDataSource
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.*
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsmeldingMeldingProvider
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.PollForVarslingsmeldingJob
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
            config.getString("database.name"),
            config.getString("database.vault.mountpath")) as DataSource
    }

    single {
        VarslingsmeldingKafkaClient(mutableMapOf(
            "bootstrap.servers" to config.getString("kafka.endpoint"),
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            SaslConfigs.SASL_MECHANISM to "PLAIN",
            SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"${config.getString("kafka.username")}\" password=\"${config.getString("kafka.password")}\";"
        ), config.getString("altinn_melding.kafka_topic")) as ManglendeInntektsmeldingMeldingProvider
    }

    single {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
            config.getString("altinn_melding.pep_gw_endpoint")
        )

        val sts = wsStsClient(
            config.getString("sts_url"),
            config.getString("service_user.username") to config.getString("service_user.password")
        )

        sts.configureFor(altinnMeldingWsClient)

        altinnMeldingWsClient
    }

    single { VarslingMapper(get()) }

    single {
        AltinnVarselSender(
            get(),
            AltinnVarselMapper(config.getString("altinn_melding.service_id")),
            get(),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
        ) as VarslingSender
    }

    //single { MockVarslingSender(get()) as VarslingSender }

    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) as DokarkivKlient }
    single {
        AltinnReadReceiptClient(
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password"),
            config.getString("altinn_melding.service_id"),
            get()
        ) as ReadReceiptProvider
    }

    single { PostgresVarslingRepository(get()) as VarslingRepository }
    single { PostgresVentendeBehandlingerRepository(get()) as VentendeBehandlingerRepository }
    single { RestSTSAccessTokenProvider(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url"), get()) } bind AccessTokenProvider::class
    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get()) } bind PdlClient::class

    single { VarslingService(get(), get(), get(), get(), get(), get(), AllowAll()) }

    single { PollForVarslingsmeldingJob(get(), get()) }
    single { SendVarslingJob(get(), get()) }
    single { UpdateReadStatusJob(get(), get())}

    single { PostgresAltinnBrevUtsendelseRepository(get()) as AltinnBrevUtsendelseRepository }
    single { PostgresAltinnBrevmalRepository(get(), get()) as AltinnBrevMalRepository }
    single { AltinnBrevutsendelseSenderImpl(get(), get(), get(),
        config.getString("altinn_melding.username"),
        config.getString("altinn_melding.password")
    ) as AltinnBrevutsendelseSender
    }
    single { SendAltinnBrevUtsendelseJob(get(), get()) }
}