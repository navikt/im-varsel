package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import kotlin.random.Random

class DummyReadReceiptProvider : ReadReceiptProvider {
    override fun isRead(varsel: Varsling): Boolean {
        return Random.nextBoolean()
    }
}