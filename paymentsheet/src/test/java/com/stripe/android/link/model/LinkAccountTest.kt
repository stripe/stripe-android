package com.stripe.android.link.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

class LinkAccountTest {

    @get:Rule
    val forceOnelinkConsumerRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.forceOnelinkConsumer,
        isEnabled = false,
    )

    @Test
    fun `Returns unredacted E164 phone number if unredacted local number and country are available`() {
        val linkAccount = makeLinkAccount(
            unredactedPhoneNumber = "(555) 555-0007",
            phoneNumberCountry = "US",
        )
        assertThat(linkAccount.unredactedPhoneNumber).isEqualTo("+15555550007")
    }

    @Test
    fun `Returns no unredacted E164 phone number if unredacted local number is missing`() {
        val linkAccount = makeLinkAccount(
            unredactedPhoneNumber = null,
            phoneNumberCountry = "US",
        )
        assertThat(linkAccount.unredactedPhoneNumber).isNull()
    }

    @Test
    fun `Returns no unredacted E164 phone number if country is missing`() {
        val linkAccount = makeLinkAccount(
            unredactedPhoneNumber = "(555) 555-0007",
            phoneNumberCountry = null,
        )
        assertThat(linkAccount.unredactedPhoneNumber).isNull()
    }

    @Test
    fun `isVerified returns true when meetsMinimumAuthenticationLevel is true and no SMS sessions`() {
        val consumerSession = ConsumerSession(
            clientSecret = "consumer_session_007",
            emailAddress = "John Doe",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            verificationSessions = emptyList(),
            currentAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
            minimumAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
        )
        val linkAccount = LinkAccount(consumerSession)
        assertThat(linkAccount.isVerified).isTrue()
    }

    @Test
    fun `isVerified returns false when auth levels are null even with verified SMS session`() {
        val consumerSession = ConsumerSession(
            clientSecret = "consumer_session_007",
            emailAddress = "John Doe",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = ConsumerSession.VerificationSession.SessionType.Sms,
                    state = ConsumerSession.VerificationSession.SessionState.Verified,
                )
            ),
            currentAuthenticationLevel = null,
            minimumAuthenticationLevel = null,
        )
        val linkAccount = LinkAccount(consumerSession)
        assertThat(linkAccount.isVerified).isFalse()
    }

    @Test
    fun `linkBrand is null when consumerSession has no linkBrand`() {
        val linkAccount = LinkAccount(makeConsumerSession())
        assertThat(linkAccount.linkBrand).isNull()
    }

    @Test
    fun `linkBrand returns consumer session brand when present`() {
        val linkAccount = LinkAccount(makeConsumerSession(linkBrand = LinkBrand.Onelink))
        assertThat(linkAccount.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `linkBrand returns Onelink when forceOnelinkConsumer flag is enabled`() {
        forceOnelinkConsumerRule.setEnabled(true)
        val linkAccount = LinkAccount(makeConsumerSession(linkBrand = LinkBrand.Link))
        assertThat(linkAccount.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `consumerLinkBrand returns raw value from consumer session`() {
        val linkAccount = LinkAccount(makeConsumerSession(linkBrand = LinkBrand.Onelink))
        assertThat(linkAccount.consumerLinkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `consumerLinkBrand returns null when session has no linkBrand`() {
        val linkAccount = LinkAccount(makeConsumerSession())
        assertThat(linkAccount.consumerLinkBrand).isNull()
    }

    private fun makeConsumerSession(
        linkBrand: LinkBrand? = null,
        isVerified: Boolean = false,
    ): ConsumerSession {
        val authLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication.takeIf { isVerified }
        return ConsumerSession(
            clientSecret = "consumer_session_007",
            emailAddress = "test@example.com",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            verificationSessions = emptyList(),
            currentAuthenticationLevel = authLevel,
            minimumAuthenticationLevel = authLevel,
            linkBrand = linkBrand,
        )
    }

    private fun makeLinkAccount(
        unredactedPhoneNumber: String?,
        phoneNumberCountry: String?,
    ): LinkAccount {
        val consumerSession = ConsumerSession(
            clientSecret = "consumer_session_007",
            emailAddress = "John Doe",
            redactedPhoneNumber = "+1********07",
            redactedFormattedPhoneNumber = "(***) *** **07",
            unredactedPhoneNumber = unredactedPhoneNumber,
            phoneNumberCountry = phoneNumberCountry,
            verificationSessions = emptyList(),
        )

        return LinkAccount(consumerSession)
    }
}
