package no.nav.helse.inntektsmeldingsvarsel


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AllowListTest {
    @Test
    internal fun `PilotAllow tillater ikke hva som helst`() {
        val allowAll = AllowAll()
        assertThat(allowAll.isAllowed("12341234")).isTrue()
    }

}