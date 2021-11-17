package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import java.time.LocalDateTime
import java.util.*

interface AltinnBrevUtsendelseRepository {
    fun insertUtsendelse(altinnMalId: UUID, virksomhetsnummere: Set<String>)
    fun getNextBatch(): List<AltinnBrevUtesendelse>
    fun updateSentStatus(id: Int, timeOfUpdate: LocalDateTime, status: Boolean)
}
