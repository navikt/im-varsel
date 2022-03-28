package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling

interface ReadReceiptProvider {
    fun isRead(varsel: Varsling): Boolean
}
