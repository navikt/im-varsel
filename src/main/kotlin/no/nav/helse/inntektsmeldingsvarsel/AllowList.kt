package no.nav.helse.inntektsmeldingsvarsel

interface AllowList {
    fun isAllowed(virksomhetsnummer: String): Boolean
}

class AllowAll : AllowList {
    override fun isAllowed(virksomhetsnummer: String): Boolean {
        return true
    }
}

class PilotAllowList(private val allowDigits: Set<Char>) : AllowList {
    override fun isAllowed(virksomhetsnummer: String): Boolean {
        return allowDigits.contains(virksomhetsnummer[5])
    }
}
