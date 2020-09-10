package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository

import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresPermisjonsvarselRepository(private val ds: DataSource) : PermisjonsvarselRepository {

    private val tableName = "permisjonsvarsel"
    private val logger = LoggerFactory.getLogger(PostgresPermisjonsvarselRepository::class.java)

    private val updatesentStatement = "UPDATE $tableName SET sent = ?, behandlet = ? WHERE id = ?"

    private val getBySentState = "SELECT * FROM $tableName WHERE sent=? LIMIT ?"

    override fun getNextBatch(): List<PermisjonsVarselDbEntity> {
        ds.connection.use {
            val resultList = ArrayList<PermisjonsVarselDbEntity>()
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


    private fun mapToDto(res: ResultSet): PermisjonsVarselDbEntity {
        return PermisjonsVarselDbEntity(
                id = res.getInt("id"),
                sent = res.getBoolean("sent"),
                read = res.getBoolean("read"),
                behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
                virksomhetsNr = res.getString("virksomhetsNr")
        )
    }

}