package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling

class MockReadReceiptProvider : ReadReceiptProvider {
    override fun isRead(varsel: Varsling): Boolean {
        return true
    }
}
