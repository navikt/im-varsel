package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import java.sql.Connection
import java.time.LocalDateTime

interface VarslingRepository {
    // for å hente ut alle aggregat i en gitt status
    fun findBySentStatus(status: Boolean, max: Int): List<VarslingDbEntity>

    fun findSentButUnread(max: Int): List<VarslingDbEntity>

    // sette inn nytt varsel
    fun insert(varsling: VarslingDbEntity, connection: Connection)

    fun remove(uuid: String)

    // for å sette status til sendt når melding for aggregatet er sendt
    fun updateSentStatus(uuid: String, timeOfUpdate: LocalDateTime, status: Boolean)
    fun updateData(uuid: String, data: String)

    fun updateReadStatus(uuid: String, readStatus: Boolean)
}
