package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse

import no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository.AltinnBrevUtesendelse

class MockAltinnBrevutsendelseSender : AltinnBrevutsendelseSender {
    override fun send(utsendelse: AltinnBrevUtesendelse) {
        System.out.println("Sendte ${utsendelse.id}")
    }
}
