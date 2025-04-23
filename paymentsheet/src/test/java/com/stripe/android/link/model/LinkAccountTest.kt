package com.stripe.android.link.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerSession
import org.junit.Test

class LinkAccountTest {

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
