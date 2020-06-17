package no.nav.helse.inntektsmeldingsvarsel

interface AllowList {
    fun shouldSend(virksomhetsnummer: String): Boolean
}

class ApproveAllAllowList : AllowList {
    override fun shouldSend(virksomhetsnummer: String): Boolean {
        return true
    }
}

class ResourceFileAllowList(resourceFilePath: String) : AllowList {
    val allowList = this::class.java.getResource(resourceFilePath)
            .readText()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    override fun shouldSend(virksomhetsnummer: String): Boolean {
        return allowList.contains(virksomhetsnummer)
    }
}