package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import org.junit.Test

class EffectiveLinkBrandTest {

    @Test
    fun `returns account linkBrand when account is present`() {
        val config = TestFactory.LINK_CONFIGURATION.copy(linkBrand = LinkBrand.Link)
        val account = LinkAccount(makeConsumerSession(linkBrand = LinkBrand.Notlink))

        assertThat(config.effectiveLinkBrand(account)).isEqualTo(LinkBrand.Notlink)
    }

    @Test
    fun `returns config linkBrand when account is null`() {
        val config = TestFactory.LINK_CONFIGURATION.copy(linkBrand = LinkBrand.Notlink)

        assertThat(config.effectiveLinkBrand(null)).isEqualTo(LinkBrand.Notlink)
    }

    @Test
    fun `returns account default Link when account session has no linkBrand`() {
        val config = TestFactory.LINK_CONFIGURATION.copy(linkBrand = LinkBrand.Notlink)
        val account = LinkAccount(makeConsumerSession(linkBrand = null))

        // LinkAccount.linkBrand defaults to Link when consumerSession.linkBrand is null
        assertThat(config.effectiveLinkBrand(account)).isEqualTo(LinkBrand.Link)
    }

    private fun makeConsumerSession(linkBrand: LinkBrand?): ConsumerSession {
        return ConsumerSession(
            clientSecret = "secret_123",
            emailAddress = "test@example.com",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            verificationSessions = emptyList(),
            linkBrand = linkBrand,
        )
    }
}
