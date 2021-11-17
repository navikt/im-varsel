package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

class MockVarslingRepository() : VarslingRepository {

    private val varsling1 = VarslingDbEntity(data = "[]", read = false, uuid = UUID.randomUUID().toString(), sent = false, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789")
    private val varsling2 = VarslingDbEntity(data = "[]", read = false, uuid = UUID.randomUUID().toString(), sent = true, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789")
    private val varsling3 = VarslingDbEntity(data = "[]", read = false, uuid = UUID.randomUUID().toString(), sent = false, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789")

    val list = listOf(varsling1, varsling2, varsling3).toMutableList()

    override fun findBySentStatus(status: Boolean, max: Int): List<VarslingDbEntity> {
        return list
    }

    override fun findSentButUnread(max: Int): List<VarslingDbEntity> {
        return listOf(varsling2)
    }

    override fun insert(varsling: VarslingDbEntity, connection: Connection) {
    }

    override fun remove(uuid: String) {
        println("Slettet $uuid")
    }

    override fun updateSentStatus(uuid: String, timeOfUpdate: LocalDateTime, sent: Boolean) {
        println("updateStatus $uuid $sent")
    }

    override fun updateData(uuid: String, data: String) {
    }

    override fun updateReadStatus(uuid: String, readStatus: Boolean) {
    }
}
