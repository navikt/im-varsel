package no.nav.helse.inntektsmeldingsvarsel.dependencyinjection

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
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.RestSTSAccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.inntektsmeldingsvarsel.*
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.AltinnBrevutsendelseSenderImpl
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.MockAltinnBrevutsendelseSender
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.SendAltinnBrevUtsendelseJob
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevMalRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevUtsendelseRepository
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.PostgresAltinnBrevmalRepository
import no.nav.helse.inntektsmeldingsvarsel.db.createHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.getDataSource
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVentendeBehandlingerRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.varsling.*
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsmeldingMeldingProvider
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.PollForVarslingsmeldingJob
import org.apache.cxf.frontend.ClientProxy
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource


@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "TEST" -> buildAndTestConfig()
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

val common = module {
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

    single { om }

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(KotlinModule())
                registerModule(Jdk8Module())
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(SerializationFeature.INDENT_OUTPUT, true)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }
    }

    single { httpClient }

    single { KubernetesProbeManager() }
}

fun buildAndTestConfig() = module {


}

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
    single { VarslingService(get(), get(), get(), get(), get(), get(), PilotAllowList(setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'))) }

    single { MockVarslingSender(get()) as VarslingSender }
    single { PollForVarslingsmeldingJob(get(), get()) }
    single { SendVarslingJob(get(), get()) }
    single { UpdateReadStatusJob(get(), get()) }

    single { PostgresAltinnBrevUtsendelseRepository(get()) as AltinnBrevUtsendelseRepository }
    single { PostgresAltinnBrevmalRepository(get(), get()) as AltinnBrevMalRepository }
    single { MockAltinnBrevutsendelseSender() as AltinnBrevutsendelseSender }
    single { SendAltinnBrevUtsendelseJob(get(), get()) }
}

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

//    single {
//        AltinnVarselSender(
//                get(),
//                AltinnVarselMapper(config.getString("altinn_melding.service_id")),
//                get(),
//                get(),
//                config.getString("altinn_melding.username"),
//                config.getString("altinn_melding.password")
//        ) as VarslingSender
//    }

    single { MockVarslingSender(get()) as VarslingSender }

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

    single { VarslingService(get(), get(), get(), get(), get(), get(), PilotAllowList(setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'))) }

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

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
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

    single { PostgresVarslingRepository(get()) as VarslingRepository }
    single { PostgresVentendeBehandlingerRepository(get()) as VentendeBehandlingerRepository }

    single { VarslingMapper(get()) }
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) as DokarkivKlient }

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

    single { RestSTSAccessTokenProvider(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url"), get()) } bind AccessTokenProvider::class
    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get() ) } bind PdlClient::class

    single { VarslingService(get(), get(), get(), get(), get(), get(), PilotAllowList(setOf('1'))) }
    single {
        AltinnReadReceiptClient(
                get(),
                config.getString("altinn_melding.username"),
                config.getString("altinn_melding.password"),
                config.getString("altinn_melding.service_id"),
                get()
        ) as ReadReceiptProvider
    }

    single { PollForVarslingsmeldingJob(get(), get()) }
    single { SendVarslingJob(get(), get()) }

    single { UpdateReadStatusJob(get(), get()) }


    single { PostgresAltinnBrevUtsendelseRepository(get()) as AltinnBrevUtsendelseRepository }
    single { PostgresAltinnBrevmalRepository(get(), get()) as AltinnBrevMalRepository }

    single { AltinnBrevutsendelseSenderImpl(get(), get(), get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
    ) as AltinnBrevutsendelseSender
    }
    single { SendAltinnBrevUtsendelseJob(get(), get()) }


}

// utils
@KtorExperimentalAPI
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format("jdbc:postgresql://%s:%s/%s",
            this.property("database.host").getString(),
            this.property("database.port").getString(),
            this.property("database.name").getString())
}

inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }
