package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

import java.time.LocalDateTime

data class VarslingDbEntity(
    val data: String,
    val uuid: String,
    val sent: Boolean,
    val read: Boolean,
    val opprettet: LocalDateTime,
    val behandlet: LocalDateTime? = null,
    val lestTidspunkt: LocalDateTime? = null,
    val virksomhetsNr: String,
    val virksomhetsNavn: String,
    val journalpostId: String? = null
)
