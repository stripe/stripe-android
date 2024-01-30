package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class InlineSignupViewStateTest {

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "US",
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        )

        val viewState = InlineSignupViewState.create(linkConfig)

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Phone,
        )
    }

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for non-US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        )

        val viewState = InlineSignupViewState.create(linkConfig)

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Phone,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Limits prefill if showing alongside save-for-future-use if all fields have prefills`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
        )

        val viewState = InlineSignupViewState.create(linkConfig)

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Correct prefill if showing alongside save-for-future-use if not all fields have prefills`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            email = null,
        )

        val viewState = InlineSignupViewState.create(linkConfig)

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Phone,
            LinkSignupField.Name,
        )
    }

    private fun createLinkConfig(
        countryCode: String,
        signupMode: LinkSignupMode,
        email: String? = "john@doe.ca",
    ): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = PaymentIntentFactory.create(countryCode = countryCode),
            signupMode = signupMode,
            merchantName = "Merchant, Inc.",
            merchantCountryCode = "usd",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "John Doe",
                email = email,
                phone = "+15555555555",
                billingCountryCode = "usd",
            ),
            shippingValues = null,
            passthroughModeEnabled = false,
        )
    }
}
