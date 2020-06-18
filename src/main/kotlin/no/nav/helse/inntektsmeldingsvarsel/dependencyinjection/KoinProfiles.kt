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
import no.nav.helse.inntektsmeldingsvarsel.*
import no.nav.helse.inntektsmeldingsvarsel.db.createHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig
import no.nav.helse.inntektsmeldingsvarsel.db.getDataSource
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.MeldingsfilterRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresMeldingsfilterRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository.VarslingRepository
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.DokarkivKlientImpl
import no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv.MockDokarkivKlient
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.AltinnPermisjonsVarselMapper
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.AltinnPermisjonsVarselSender
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.SendPermitteringsMeldingJob
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsvarselRepository
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PostgresPermisjonsvarselRepository
import no.nav.helse.inntektsmeldingsvarsel.pdl.PdlClient
import no.nav.helse.inntektsmeldingsvarsel.varsling.*
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.ManglendeInntektsmeldingMeldingProvider
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingKafkaClient
import no.nav.helse.inntektsmeldingsvarsel.varsling.mottak.VarslingsmeldingProcessor
import org.apache.cxf.ext.logging.LoggingInInterceptor
import org.apache.cxf.ext.logging.LoggingOutInterceptor
import org.apache.cxf.frontend.ClientProxy
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
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

    single { PostgresVarslingRepository(get()) as VarslingRepository }
    single { PostgresMeldingsfilterRepository(get()) as MeldingsfilterRepository }
    single { VarslingService(get(), get(), get(), get(), get()) }

    single { DummyVarslingSender(get()) as VarslingSender }
    single { VarslingsmeldingProcessor(get(), get()) }
    single { SendVarslingJob(get(), get()) }
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

        //val client = ClientProxy.getClient(altinnMeldingWsClient)
        //client.inInterceptors.add(LoggingInInterceptor())
        //client.outInterceptors.add(LoggingOutInterceptor())

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
                ApproveAllAllowList(),
                get(),
                AltinnVarselMapper(config.getString("altinn_melding.service_id")),
                get(),
                config.getString("altinn_melding.username"),
                config.getString("altinn_melding.password")
        ) as VarslingSender
    }

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
    single { PostgresMeldingsfilterRepository(get()) as MeldingsfilterRepository }
    single { RestStsClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url")) }
    single { PdlClient(config.getString("pdl_url"), get(), get(), get()) }

    single { VarslingService(get(), get(), get(), get(), get()) }


    single { PostgresPermisjonsvarselRepository(get()) as PermisjonsvarselRepository }
    single { AltinnPermisjonsVarselSender(
            DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()),
            AltinnPermisjonsVarselMapper("4255"),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
    ) }
    single { SendPermitteringsMeldingJob(get(), get()) }



    single { VarslingsmeldingProcessor(get(), get()) }
    single { SendVarslingJob(get(), get()) }
    single { UpdateReadStatusJob(get(), get())}
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
        client.inInterceptors.add(LoggingInInterceptor())
        client.outInterceptors.add(LoggingOutInterceptor())

        val sts = wsStsClient(
                config.getString("sts_url"),
                config.getString("service_user.username") to config.getString("service_user.password")
        )

        sts.configureFor(altinnMeldingWsClient)

        altinnMeldingWsClient
    }

    single { VarslingMapper(get()) }

    single { PostgresVarslingRepository(get()) as VarslingRepository }
    single { PostgresMeldingsfilterRepository(get()) as MeldingsfilterRepository }

    single { RestStsClient(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url")) }
    single { PdlClient(config.getString("pdl_url"), get(), get(), get() ) }

    single { VarslingService(get(), get(), get(), get(), get()) }
    single { MockDokarkivKlient() as DokarkivKlient }

    single { DummyVarslingSender(get()) as VarslingSender}
    single { VarslingsmeldingProcessor(get(), get()) }
    single { SendVarslingJob(get(), get()) }


    single { DummyReadReceiptProvider() as ReadReceiptProvider }
    single { UpdateReadStatusJob(get(), get()) }
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