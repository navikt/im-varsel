package no.nav.helse.inntektsmeldingsvarsel.dependencyinjection

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test

import io.ktor.config.HoconApplicationConfig
import org.junit.Assert

import org.junit.jupiter.api.Assertions.*

internal class KoinProfilesKtTest {
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())
    @Test
    fun selectModuleBasedOnProfile() {
        val modules = selectModuleBasedOnProfile(appConfig)
        Assert.assertTrue(modules.size == 2)
    }
}
