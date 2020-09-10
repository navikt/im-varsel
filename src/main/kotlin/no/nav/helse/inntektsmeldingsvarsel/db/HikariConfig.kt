package no.nav.helse.inntektsmeldingsvarsel.db

import com.zaxxer.hikari.HikariConfig

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
    HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        driverClassName = "org.postgresql.Driver"
        username?.let { this.username = it }
        password?.let { this.password = it }
    }


fun createLocalHikariConfig() =
        createHikariConfig("jdbc:postgresql://localhost:5432/im-varsel", "im-varsel", "im-varsel")