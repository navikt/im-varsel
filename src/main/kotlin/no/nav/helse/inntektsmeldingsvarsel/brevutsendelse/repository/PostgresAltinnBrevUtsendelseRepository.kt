package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresAltinnBrevUtsendelseRepository(private val ds: DataSource) : AltinnBrevUtsendelseRepository {

    private val tableName = "altinn_brev_utsendelse"
    private val logger = LoggerFactory.getLogger(PostgresAltinnBrevUtsendelseRepository::class.java)

    private val insertStatement = "INSERT INTO $tableName(virksomhetsNr, altinnBrevMalId) VALUES "

    private val updatesentStatement = "UPDATE $tableName SET sent = ?, behandlet = ? WHERE id = ?:uuid"

    private val getBySentState = "SELECT * FROM $tableName WHERE sent=? LIMIT ?"

    override fun insertUtsendelse(altinnMalId: UUID, virksomhetsnummere: Set<String>) {
        val malId = altinnMalId.toString()
        val validatedNumbers = virksomhetsnummere
                .filter { orgnr -> orgnr.length == 9 && orgnr.all { it.isDigit() } }
                .joinToString { "('$it', '$malId')" }

        ds.connection.use {
            it.prepareStatement(insertStatement + validatedNumbers).executeUpdate()
        }
    }

    override fun getNextBatch(): List<AltinnBrevUtesendelse> {
        ds.connection.use {
            val resultList = ArrayList<AltinnBrevUtesendelse>()
            val res = it.prepareStatement(getBySentState).apply {
                setBoolean(1, false)
                setInt(2, 500)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList
        }
    }

    override fun updateSentStatus(id: Int, timeOfUpdate: LocalDateTime, sent: Boolean) {
        ds.connection.use {
            it.prepareStatement(updatesentStatement).apply {
                setBoolean(1, sent)
                setTimestamp(2, Timestamp.valueOf(timeOfUpdate))
                setInt(3, id)
            }.executeUpdate()
        }
    }


    private fun mapToDto(res: ResultSet): AltinnBrevUtesendelse {
        return AltinnBrevUtesendelse(
                id = res.getInt("id"),
                altinnBrevMalId = UUID.fromString(res.getString("altinnBrevMalId")),
                sent = res.getBoolean("sent"),
                behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
                virksomhetsNr = res.getString("virksomhetsNr")
        )
    }
}