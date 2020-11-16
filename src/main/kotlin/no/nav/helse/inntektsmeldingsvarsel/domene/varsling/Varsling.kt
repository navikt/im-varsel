package no.nav.helse.inntektsmeldingsvarsel.domene.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import java.time.LocalDateTime
import java.util.*

data class PersonVarsling(
        val navn: String,
        val personnumer: String,
        val periode: Periode,
        val varselOpprettet: LocalDateTime,
        var joarkRef: String? = null
)

data class Varsling(
        val virksomhetsNr: String,
        val liste: MutableSet<PersonVarsling>,
        val uuid: String = UUID.randomUUID().toString(), // Uuid sendes også til Altinn som referanse
        val opprettet: LocalDateTime = LocalDateTime.now(),
        val varslingSendt: Boolean = false,
        val varslingLest: Boolean = false
)
