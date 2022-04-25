package no.nav.helse.inntektsmeldingsvarsel.db

import org.junit.Assert
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PostgresKtTest {

    @Test
    fun getDataSource() {
    }

    @Test
    fun dataSourceFromVault() {
        val hikariDataSource = dataSourceFromVault(createLocalHikariConfig(), "im-varsel", "test", Role.user)
        Assert.assertNotNull(hikariDataSource)
    }
}
