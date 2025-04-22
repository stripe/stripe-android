package com.stripe.android.link.ui.signup

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.LinkMode
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class SignUpScreenStateTest {

    @Test
    fun `Produces correct initial state if no customer info is passed in`() {
        val customerInfo = makeCustomerInfo()

        val result = SignUpScreenState.create(
            configuration = makeLinkConfiguration(customerInfo),
            customerInfo = customerInfo,
        )

        assertThat(result.signUpEnabled).isFalse()
        assertThat(result.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
    }

    @Test
    fun `Produces correct initial state if only email is passed in`() {
        val customerInfo = makeCustomerInfo(
            email = "email@email.com",
        )

        val result = SignUpScreenState.create(
            configuration = makeLinkConfiguration(customerInfo),
            customerInfo = customerInfo,
        )

        assertThat(result.signUpEnabled).isFalse()
        assertThat(result.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
    }

    @Test
    fun `Produces correct initial state if everything but email is passed in`() {
        val customerInfo = makeCustomerInfo(
            email = null,
            phone = "5555555555",
            name = "John Doe",
            billingCountryCode = "US",
        )

        val result = SignUpScreenState.create(
            configuration = makeLinkConfiguration(customerInfo),
            customerInfo = customerInfo,
        )

        assertThat(result.signUpEnabled).isFalse()
        assertThat(result.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
    }

    @Test
    fun `Produces correct initial state if all required details are passed in`() {
        val customerInfo = makeCustomerInfo(
            email = "email@email.com",
            phone = "5555555555",
            name = "John Doe",
            billingCountryCode = "US",
        )

        val result = SignUpScreenState.create(
            configuration = makeLinkConfiguration(customerInfo),
            customerInfo = customerInfo,
        )

        assertThat(result.signUpEnabled).isTrue()
        assertThat(result.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
    }

    private fun makeLinkConfiguration(
        customerInfo: LinkConfiguration.CustomerInfo,
    ): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = PaymentIntentFactory.create(),
            merchantName = "Merchant, Inc.",
            merchantCountryCode = "US",
            customerInfo = customerInfo,
            shippingDetails = null,
            passthroughModeEnabled = true,
            flags = emptyMap(),
            cardBrandChoice = null,
            useAttestationEndpointsForLink = true,
            suppress2faModal = false,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_123_secret_456"),
            elementsSessionId = "elements_session_id_123",
            linkMode = LinkMode.Passthrough,
        )
    }

    private fun makeCustomerInfo(
        email: String? = null,
        phone: String? = null,
        name: String? = null,
        billingCountryCode: String? = null,
    ): LinkConfiguration.CustomerInfo {
        return LinkConfiguration.CustomerInfo(
            email = email,
            phone = phone,
            name = name,
            billingCountryCode = billingCountryCode,
        )
    }
}
