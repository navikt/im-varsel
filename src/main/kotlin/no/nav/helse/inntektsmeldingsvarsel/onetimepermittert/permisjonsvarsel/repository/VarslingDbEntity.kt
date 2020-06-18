package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository

import java.time.LocalDateTime

data class PermisjonsVarselDbEntity(
        val id: Int,
        val sent: Boolean,
        val read: Boolean,
        val behandlet: LocalDateTime? = null,
        val virksomhetsNr: String
)