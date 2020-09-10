package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import javax.sql.DataSource

class PostgresMeldingsfilterRepository(private val ds: DataSource): MeldingsfilterRepository{

    private val tableName = "meldingsfilter"
    private val insertStatement = "INSERT INTO $tableName (hash) VALUES(?)"
    private val findStatement = "SELECT * FROM $tableName WHERE hash=?"

    override fun insert(hash: String) {
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, hash)
            }.executeUpdate()
        }
    }

    override fun exists(hash: String): Boolean {
        ds.connection.use {
            return it.prepareStatement(findStatement).apply {
                setString(1, hash)
            }.executeQuery().next()
        }
    }
}