package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresVarslingRepository(private val ds: DataSource) : VarslingRepository {

    private val tableName = "varsling"
    private val logger = LoggerFactory.getLogger(PostgresVarslingRepository::class.java)

    private val insertStatement = "INSERT INTO $tableName (data, sent, opprettet, virksomhetsNr, uuid) VALUES(?::json, ?, ?, ?, ?::uuid)"

    private val updateDataStatement = "UPDATE $tableName SET data = ?::json WHERE uuid = ?"
    private val updatesentStatement = "UPDATE $tableName SET sent = ?, behandlet = ? WHERE uuid = ?"
    private val updateReadStatusStatement = "UPDATE $tableName SET read = ? WHERE uuid = ?"

    private val deleteStatement = "DELETE FROM $tableName WHERE uuid = ?"
    private val waitingAggregatesStatement = "SELECT * FROM $tableName WHERE sent=? LIMIT ?"
    private val selectUneadStatusStatement = "SELECT * FROM $tableName WHERE read=false and sent = true order by behandlet desc LIMIT ?"

    override fun findBySentStatus(sent: Boolean, max: Int): List<VarslingDbEntity> {
        ds.connection.use {
            val resultList = ArrayList<VarslingDbEntity>()
            val res = it.prepareStatement(waitingAggregatesStatement).apply {
                setBoolean(1, sent)
                setInt(2, max)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList
        }
    }

    override fun findSentButUnread(max: Int): List<VarslingDbEntity> {
        ds.connection.use {
            val resultList = ArrayList<VarslingDbEntity>()
            val res = it.prepareStatement(selectUneadStatusStatement).apply {
                setInt(1, max)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList
        }
    }

    override fun updateData(uuid: String, data: String) {
        ds.connection.use {
            it.prepareStatement(updateDataStatement).apply {
                setString(1, data)
                setString(2, uuid.toString())
            }.executeUpdate()
        }
    }

    override fun updateReadStatus(uuid: String, readStatus: Boolean) {
        ds.connection.use {
            it.prepareStatement(updateReadStatusStatement).apply {
                setBoolean(1, readStatus)
                setString(2, uuid)
            }.executeUpdate()
        }
    }

    override fun insert(dbEntity: VarslingDbEntity, con: Connection) {
        con.prepareStatement(insertStatement).apply {
            setString(1, dbEntity.data)
            setBoolean(2, dbEntity.sent)
            setTimestamp(3, Timestamp.valueOf(dbEntity.opprettet))
            setString(4, dbEntity.virksomhetsNr)
            setString(5, dbEntity.uuid)
        }.executeUpdate()
    }

    override fun remove(uuid: String) {
        ds.connection.use {
            it.prepareStatement(deleteStatement).apply {
                setString(1, uuid)
            }.executeUpdate()
        }
    }

    override fun updateSentStatus(uuid: String, timeOfUpdate: LocalDateTime, sent: Boolean) {
        ds.connection.use {
            it.prepareStatement(updatesentStatement).apply {
                setBoolean(1, sent)
                setTimestamp(2, Timestamp.valueOf(timeOfUpdate))
                setString(3, uuid)
            }.executeUpdate()
        }
    }

    private fun mapToDto(res: ResultSet): VarslingDbEntity {
        return VarslingDbEntity(
                data = res.getString("data"),
                uuid = res.getString("uuid"),
                sent = res.getBoolean("sent"),
                read = res.getBoolean("read"),
                opprettet = res.getTimestamp("opprettet").toLocalDateTime(),
                behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
                virksomhetsNr = res.getString("virksomhetsNr")
        )
    }
}