package no.nav.helse.inntektsmeldingsvarsel.domene

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)