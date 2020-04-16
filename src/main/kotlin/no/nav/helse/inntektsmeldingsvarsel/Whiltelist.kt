package no.nav.helse.inntektsmeldingsvarsel

interface Whitelist {
    fun shouldSend(virksomhetsnummer: String): Boolean
}

class StaticWhitelist(private val whiteList: Set<String>) : Whitelist {
    override fun shouldSend(virksomhetsnummer: String): Boolean {
        return whiteList.contains(virksomhetsnummer)
    }
}