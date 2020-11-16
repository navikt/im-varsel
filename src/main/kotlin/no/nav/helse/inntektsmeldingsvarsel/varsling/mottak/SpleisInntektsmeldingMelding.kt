package no.nav.helse.inntektsmeldingsvarsel.varsling.mottak

import java.time.LocalDate
import java.time.LocalDateTime

// https://github.com/navikt/helse-spre-arbeidsgiver/blob/fe59201a62c28749cf48f43bd226b014ab10c5d3/src/main/kotlin/no/nav/helse/App.kt#L109
data class SpleisInntektsmeldingMelding(
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val opprettet: LocalDateTime, // Dette er tidspunktet da vedtakssystemet ville ha en inntektsmelding
        val fødselsnummer: String,
        val meldingsType: Meldingstype = Meldingstype.TRENGER_INNTEKTSMELDING
) {

    enum class Meldingstype {
        TRENGER_INNTEKTSMELDING,
        TRENGER_IKKE_INNTEKTSMELDING
    }
}