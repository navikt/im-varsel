package no.nav.helse.inntektsmeldingsvarsel.varsling.mottak

import toHash
import java.time.LocalDate
import java.time.LocalDateTime

// https://github.com/navikt/helse-spre-arbeidsgiver/blob/fe59201a62c28749cf48f43bd226b014ab10c5d3/src/main/kotlin/no/nav/helse/App.kt#L109
data class ManglendeInntektsMeldingMelding(
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val opprettet: LocalDateTime, // Dette er tidspunktet da vedtakssystemet ville ha en inntektsmelding
        val fødselsnummer: String
) {
    /**
     * Gir en hash av fom, tom, fødselsnummer og organisasjonsnummer
     * Dette gir en unik nøkken for om vi har sett denne perioden for denne personen på denne organisasjonen
     * før. SPLEIS kan sende melding for samme periode når de kjører sine jobber på nytt, og vi må derfor sjekke
     * om vi har sett perioden før. "opprettet" vil være ny i slike tilfeller, så kan ikke inkluderes.
     */
    fun periodeHash(): String {
        return  "$fødselsnummer-$organisasjonsnummer-$fom-$tom".toHash("SHA-256")
    }
}