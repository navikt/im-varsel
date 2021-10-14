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
import org.apache.cxf.frontend.ClientProxy
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    val ALTINN_MELDING_USERNAME: String = config.getString("altinn_melding.username")
    val ALTINN_MELDING_PASSWORD: String =  config.getString("altinn_melding.password")
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
            config.getString("database.name"),
            config.getString("database.vault.mountpath"))
    }

    single {
        VarslingsmeldingKafkaClient(mutableMapOf(
            "bootstrap.servers" to config.getString("kafka.endpoint"),
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            SaslConfigs.SASL_MECHANISM to "PLAIN",
            SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"${config.getString("kafka.username")}\" password=\"${config.getString("kafka.password")}\";"
        ), config.getString("altinn_melding.kafka_topic"))
    }

    single {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
            config.getString("altinn_melding.pep_gw_endpoint")
        )

        val client = ClientProxy.getClient(altinnMeldingWsClient)
        //client.inInterceptors.add(LoggingInInterceptor())
        //client.outInterceptors.add(LoggingOutInterceptor())

        val sts = wsStsClient(
            config.getString("sts_url"),
            config.getString("service_user.username") to config.getString("service_user.password")
        )

        sts.configureFor(altinnMeldingWsClient)

        altinnMeldingWsClient
    }

    single { PostgresVarslingRepository(get()) }
    single { PostgresVentendeBehandlingerRepository(get()) }

    single { VarslingMapper(get()) }
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) }

    single {
        AltinnVarselSender(
            get(),
            AltinnVarselMapper(config.getString("altinn_melding.service_id")),
            get(),
            get(),
            ALTINN_MELDING_USERNAME,
            ALTINN_MELDING_PASSWORD
        )
    }

    single { RestSTSAccessTokenProvider(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url"), get()) } bind AccessTokenProvider::class
    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get() ) } bind PdlClient::class

    single { VarslingService(get(), get(), get(), get(), get(), get(), ResourceFileAllowList("/allow-list/virksomheter-allow-prod")) }
    single {
        AltinnReadReceiptClient(
            get(),
            ALTINN_MELDING_USERNAME,
            ALTINN_MELDING_PASSWORD,
            config.getString("altinn_melding.service_id"),
            get()
        )
    }

    single { PollForVarslingsmeldingJob(get(), get()) }
    single { SendVarslingJob(get(), get()) }

    single { UpdateReadStatusJob(get(), get()) }


    single { PostgresAltinnBrevUtsendelseRepository(get()) }
    single { PostgresAltinnBrevmalRepository(get(), get()) }

    single {
        AltinnBrevutsendelseSenderImpl(get(), get(), get(),
            ALTINN_MELDING_USERNAME,
            ALTINN_MELDING_PASSWORD
        )
    }
    single { SendAltinnBrevUtsendelseJob(get(), get()) }


}
