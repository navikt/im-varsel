package no.nav.helse.inntektsmeldingsvarsel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AllowListTest {
    @Test
    internal fun `AllowAll tillater hva som helst`() {
        val allowAll = AllowAll()
        assertThat(allowAll.isAllowed("12341234")).isTrue
    }

    @Test
    internal fun `PilotAllow tillater riktig organiasjonsnummer`() {
        val pilotAllowList = PilotAllowList(setOf('1'))
        assertThat(pilotAllowList.isAllowed("12345134")).isTrue
    }

    @Test
    internal fun `PilotAllow tillater riktig organiasjonsnummer liste`() {
        val pilotAllowList = PilotAllowList(setOf('0', '1', '2', '3'))
        assertThat(pilotAllowList.isAllowed("00000000")).isTrue
        assertThat(pilotAllowList.isAllowed("00000100")).isTrue
        assertThat(pilotAllowList.isAllowed("00000200")).isTrue
        assertThat(pilotAllowList.isAllowed("00000300")).isTrue
        assertThat(pilotAllowList.isAllowed("00000400")).isFalse
    }

    @Test
    internal fun `PilotAllow tillater ikke feil organiasjonsnummer`() {
        val pilotAllowList = PilotAllowList(setOf('1'))
        assertThat(pilotAllowList.isAllowed("12345234")).isFalse
    }
}
