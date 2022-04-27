package no.nav.helse.inntektsmeldingsvarsel

import no.nav.helse.inntektsmeldingsvarsel.domene.Periode
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime

internal class AltinnVarselMapperTest {

    private val varsling = Varsling(
        virksomhetsNr = "123456785",
        virksomhetsNavn = "Stark Industries",
        liste = mutableSetOf(PersonVarsling("Ole", "123", Periode(LocalDate.now(), LocalDate.now()), LocalDateTime.now()))
    )
    @Test
    fun mapVarslingTilInsertCorrespondence() {
        val altinnVarselMapper = AltinnVarselMapper("5534")

        val insertCorrespondenceV2 = altinnVarselMapper.mapVarslingTilInsertCorrespondence(varsling)
        assertEquals("5534", insertCorrespondenceV2.serviceCode)
        assertEquals("1", insertCorrespondenceV2.serviceEdition)
        assertEquals("123456785", insertCorrespondenceV2.reportee)
        assertEquals("NAV (Arbeids- og velferdsetaten)", insertCorrespondenceV2.messageSender)
    }

    @Test
    fun getAltinnTjenesteKode() {
    }
}
