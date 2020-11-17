package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.inntektsmeldingsvarsel.db.createLocalHikariConfig

fun clearAllDatabaseTables() {
    val dataSource = HikariDataSource(createLocalHikariConfig())
    dataSource.connection.use {
        val allTables = it.prepareStatement("SELECT tablename\n" +
                "FROM pg_tables\n" +
                "WHERE schemaname = 'public';").executeQuery()

        while(allTables.next()) {
            it.prepareStatement("DELETE FROM ${allTables.getString(1)}").executeUpdate()
        }
    }
}