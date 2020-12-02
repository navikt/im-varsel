package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.ANTALL_PERSONER_I_SENDTE_VARSLER
import no.nav.helse.inntektsmeldingsvarsel.ANTALL_SENDTE_VARSLER
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling

class MockVarslingSender(private val service: VarslingService) : VarslingSender {
    override fun send(varsling: Varsling) {
        println("Sender varsling med id ${varsling.uuid} med ${varsling.liste.size} personer i")
        service.oppdaterSendtStatus(varsling, true)
        ANTALL_SENDTE_VARSLER.inc()
        ANTALL_PERSONER_I_SENDTE_VARSLER.inc(varsling.liste.size.toDouble())
    }
}