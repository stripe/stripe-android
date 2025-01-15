package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class InlineSignupViewPaymentMethodStateTest {

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "US",
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Phone,
        )
    }

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for non-US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        )

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
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Correct prefill if showing alongside save-for-future-use if not all fields have prefills`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
            email = null,
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Phone,
            LinkSignupField.Name,
        )
    }

    private fun createLinkConfig(
        countryCode: String,
        email: String? = "john@doe.ca",
    ): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = PaymentIntentFactory.create(countryCode = countryCode),
            merchantName = "Merchant, Inc.",
            merchantCountryCode = "usd",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "John Doe",
                email = email,
                phone = "+15555555555",
                billingCountryCode = "usd",
            ),
            shippingDetails = null,
            passthroughModeEnabled = false,
            flags = emptyMap(),
            cardBrandChoice = null,
        )
    }
}
