package no.nav.helse.inntektsmeldingsvarsel.varsling

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling

class DummyVarslingSender(private val service: VarslingService) : VarslingSender {
    override fun
            send(varsling: Varsling) {
        println("Sender varsling med id ${varsling.uuid} til {${varsling.virksomhetsNr} med ${varsling.liste.size} personer i")
        service.oppdaterStatus(varsling, true)
    }
}