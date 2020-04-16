package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling

interface VarslingSender {
    fun send(varsling: Varsling)
}

