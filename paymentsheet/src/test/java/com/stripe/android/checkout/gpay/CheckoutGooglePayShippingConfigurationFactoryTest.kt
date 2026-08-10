package com.stripe.android.checkout.gpay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutGooglePayShippingConfigurationFactoryTest {
    @Test
    fun `returns shipping configuration when express checkout requires shipping address`() {
        val state = CheckoutControllerStateFactory.create(
            configuration = CheckoutController.Configuration()
                .expressCheckoutElement(
                    ExpressCheckoutElement.Configuration()
                        .shippingAddressRequired(true),
                )
                .build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                allowedShippingCountries = listOf("US", "CA"),
            ),
        )

        val configuration = CheckoutGooglePayShippingConfigurationFactory.create(state)

        assertThat(configuration).isNotNull()
        assertThat(configuration?.shippingOptionsParameters).isNull()
    }

    @Test
    fun `returns shipping configuration when checkout session collects shipping address`() {
        val state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                allowedShippingCountries = listOf("US"),
                shippingOptions = listOf(
                    CheckoutSessionResponse.ShippingRate(
                        id = "shr_standard",
                        amount = 500L,
                        displayName = "Standard Shipping",
                        deliveryEstimate = null,
                    ),
                ),
            ),
        )

        val configuration = CheckoutGooglePayShippingConfigurationFactory.create(state)

        assertThat(configuration).isNotNull()
        assertThat(configuration?.shippingOptionsParameters?.shippingOptions).hasSize(1)
    }

    @Test
    fun `returns null when shipping is not required`() {
        val state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                allowedShippingCountries = null,
            ),
        )

        assertThat(CheckoutGooglePayShippingConfigurationFactory.create(state)).isNull()
    }
}
