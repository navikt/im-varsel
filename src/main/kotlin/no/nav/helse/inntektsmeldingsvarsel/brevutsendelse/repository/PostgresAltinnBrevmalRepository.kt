package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource
import kotlin.collections.HashSet

class PostgresAltinnBrevmalRepository(private val ds: DataSource, private val om: ObjectMapper) : AltinnBrevMalRepository {

    private val tableName = "altinn_brev_mal"
    private val logger = LoggerFactory.getLogger(PostgresAltinnBrevmalRepository::class.java)

    private val insertStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val updateStatement = "UPDATE $tableName SET data = ?::json where data ->> 'id' = ?"

    private val getById = "SELECT data FROM $tableName WHERE data ->> 'id' = ?"

    private val getAll = "SELECT data FROM $tableName"

    override fun getAll(): Set<AltinnBrevMal> {
        ds.connection.use {
            val resultList = HashSet<AltinnBrevMal>()
            val res = it.prepareStatement(getAll).executeQuery()
            while (res.next()) {
                resultList.add(om.readValue(res.getString("data")))
            }
            return resultList
        }
    }

    override fun get(id: UUID): AltinnBrevMal {
        ds.connection.use {
            val res = it.prepareStatement(getById).apply {
                setString(1, id.toString())
            }.executeQuery()

            if (!res.next()) {
                throw IllegalArgumentException("$id does not exist")
            }

            return om.readValue(res.getString("data"))
        }
    }

    override fun insert(mal: AltinnBrevMal) {
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, om.writeValueAsString(mal))
            }.executeUpdate()
        }
    }

    override fun update(mal: AltinnBrevMal) {
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, om.writeValueAsString(mal))
                setString(2, mal.id.toString())
            }.executeUpdate()
        }
    }
}