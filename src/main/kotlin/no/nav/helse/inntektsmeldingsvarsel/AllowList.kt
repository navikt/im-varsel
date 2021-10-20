package no.nav.helse.inntektsmeldingsvarsel

interface AllowList {
    fun isAllowed(virksomhetsnummer: String): Boolean
}

class AllowAll : AllowList {
    override fun isAllowed(virksomhetsnummer: String): Boolean {
        return true
    }
}

//class ResourceFileAllowList(resourceFilePath: String) : AllowList {
//    val allowList = this::class.java.getResource(resourceFilePath)
//            .readText()
//            .split("\n")
//            .map { it.trim() }
//            .filter { it.isNotEmpty() }
//
//    override fun isAllowed(virksomhetsnummer: String): Boolean {
//        return allowList.contains(virksomhetsnummer)
//    }
//}

class PilotAllowList(private val allowDigits: Set<Char>) : AllowList {
    override fun isAllowed(virksomhetsnummer: String): Boolean {
        return allowDigits.contains(virksomhetsnummer[5])
    }
}