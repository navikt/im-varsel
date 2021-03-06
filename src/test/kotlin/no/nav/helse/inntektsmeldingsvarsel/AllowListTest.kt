package no.nav.helse.inntektsmeldingsvarsel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AllowListTest {
    @Test
    internal fun `AllowAll tillater hva som helst`() {
        val allowAll = AllowAll()
        assertThat(allowAll.isAllowed("1234")).isTrue()
        assertThat(allowAll.isAllowed("")).isTrue()
    }

    @Test
    internal fun `ResourceFileAllowList leser fra en resource fil og tillater kun oppføringer på linjer i filen`() {
        val allowList = ResourceFileAllowList("/allow-list-test")
        assertThat(allowList.isAllowed("974778725")).isTrue()
        assertThat(allowList.isAllowed("1234566789")).isFalse()
        assertThat(allowList.isAllowed("")).isFalse()
    }
}