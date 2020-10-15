package no.nav.helse

import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtesendelse
import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevmal
import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TestData {
    val Varsling = Varsling(
            aggregatperiode = "P-W52",
            virksomhetsNr = "123456785",
            liste = mutableSetOf(PersonVarsling("Ole", "123", Periode(LocalDate.now(), LocalDate.now()), LocalDateTime.now()))
    )

    val AltinnBrevmal = AltinnBrevmal(
        UUID.randomUUID(),
        "Dette er en tittel",
        "Dette er et sammendrag",
        "Dette er brevet",
        "2290",
        "1",
        "SYK",
        "Tittelen på dokumentet i joark",
        "brevkode_i_joark"
    )


    val AltinnBrevutsendelse_Ubehandlet = AltinnBrevUtesendelse(
        1,
            AltinnBrevmal.id,
            false,
            "123456785",
    )
}

