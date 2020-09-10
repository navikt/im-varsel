package no.nav.helse.inntektsmeldingsvarsel.domene

data class Arbeidsgiver(
        val navn: String,
        val organisasjonsnummer: String?,
        val arbeidsgiverId: String
)