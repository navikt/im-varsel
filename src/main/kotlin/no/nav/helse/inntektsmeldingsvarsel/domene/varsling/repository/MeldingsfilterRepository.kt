package no.nav.helse.inntektsmeldingsvarsel.domene.varsling.repository

/**
 * Inneholder en liste over hasher av meldinger som allerede har blitt prosessert
 */
interface MeldingsfilterRepository {
    fun insert(hash: String)
    fun exists(hash: String): Boolean
}