package com.stripe.android.paymentelement.embedded.linkinline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthRegistry.Companion.AUTO_SEND_COOLDOWN_MILLIS
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.IneligibleReason
import org.junit.Test

internal class EmbeddedLinkInlineAuthRegistryTest {
    private var now = 1_000L
    private val registry = EmbeddedLinkInlineAuthRegistry(now = { now })

    @Test
    fun `new email is eligible`() {
        assertThat(registry.ineligibleReason(EMAIL, OWNER)).isNull()
    }

    @Test
    fun `dismissal is remembered regardless of case`() {
        registry.recordDismissal("Jenny@Example.com")

        assertThat(registry.ineligibleReason("jenny@example.COM", OWNER))
            .isEqualTo(IneligibleReason.DismissedThisSession)
    }

    @Test
    fun `dismissal does not affect another email`() {
        registry.recordDismissal(EMAIL)

        assertThat(registry.ineligibleReason("other@example.com", OWNER)).isNull()
    }

    @Test
    fun `claim makes other owners ineligible`() {
        registry.claim(OWNER)

        assertThat(registry.ineligibleReason(EMAIL, OTHER_OWNER)).isEqualTo(IneligibleReason.ShownElsewhere)
    }

    @Test
    fun `claim keeps the owner eligible`() {
        registry.claim(OWNER)

        assertThat(registry.ineligibleReason(EMAIL, OWNER)).isNull()
    }

    @Test
    fun `release by the owner makes others eligible again`() {
        registry.claim(OWNER)

        registry.release(OWNER)

        assertThat(registry.ineligibleReason(EMAIL, OTHER_OWNER)).isNull()
    }

    @Test
    fun `release by another owner keeps the claim`() {
        registry.claim(OWNER)

        registry.release(OTHER_OWNER)

        assertThat(registry.ineligibleReason(EMAIL, OTHER_OWNER)).isEqualTo(IneligibleReason.ShownElsewhere)
    }

    @Test
    fun `auto send blocks the email until the cooldown ends`() {
        registry.recordAutoSend("Jenny@Example.com")

        now += AUTO_SEND_COOLDOWN_MILLIS - 1

        assertThat(registry.ineligibleReason(EMAIL, OWNER)).isEqualTo(IneligibleReason.CodeRecentlySent)
    }

    @Test
    fun `auto send stops blocking the email once the cooldown ends`() {
        registry.recordAutoSend(EMAIL)

        now += AUTO_SEND_COOLDOWN_MILLIS

        assertThat(registry.ineligibleReason(EMAIL, OWNER)).isNull()
    }

    @Test
    fun `cooldown lasts ten minutes`() {
        assertThat(AUTO_SEND_COOLDOWN_MILLIS).isEqualTo(10 * 60 * 1000L)
    }

    @Test
    fun `reset forgets dismissals, sends and the owner`() {
        registry.recordDismissal(EMAIL)
        registry.recordAutoSend(EMAIL)
        registry.claim(OWNER)

        registry.reset()

        assertThat(registry.ineligibleReason(EMAIL, OTHER_OWNER)).isNull()
    }

    private companion object {
        const val EMAIL = "jenny@example.com"
        val OWNER = Any()
        val OTHER_OWNER = Any()
    }
}
