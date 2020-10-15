package no.nav.helse.inntektsmeldingsvarsel.brevutsendelse.repository

import java.util.*

interface AltinnBrevMalRepository {
    fun getAll(): Set<AltinnBrevMal>
    fun get(id: UUID): AltinnBrevMal
    fun insert(mal: AltinnBrevMal)
    fun update(mal: AltinnBrevMal)
}