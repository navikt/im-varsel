package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository

import java.time.LocalDateTime

interface PermisjonsvarselRepository {
    fun getNextBatch(): List<PermisjonsVarselDbEntity>
    fun updateSentStatus(id: Int, timeOfUpdate: LocalDateTime, status: Boolean)
}