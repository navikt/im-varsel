package no.nav.helse.inntektsmeldingsvarsel.domene

data class Person(
        val fornavn: String,
        val etternavn: String,
        val identitetsnummer: String
)