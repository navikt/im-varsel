package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import java.time.LocalDateTime
import java.util.*

data class AltinnBrevUtesendelse(
        val id: Int,
        val altinnBrevMalId: UUID,
        val sent: Boolean,
        val virksomhetsNr: String,
        val behandlet: LocalDateTime? = null,
        val joarkRef: String? = null
)

data class AltinnBrevmal(
        val id: UUID = UUID.randomUUID(),
        val header: String,
        val summary: String,
        val bodyHtml: String,
        val altinnTjenestekode: String,
        val altinnTjenesteVersjon: String,

        val joarkTema: String,
        val joarkTittel: String,
        val joarkBrevkode: String
)