package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import java.util.*

interface AltinnBrevMalRepository {
    fun getAll(): Set<AltinnBrevmal>
    fun get(id: UUID): AltinnBrevmal
    fun insert(mal: AltinnBrevmal)
    fun update(mal: AltinnBrevmal)
}
