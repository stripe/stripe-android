package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration defaults link visibility to auto and google pay to null`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Auto
        )
        assertThat(state.googlePayConfiguration).isNull()
    }

    @Test
    fun `configuration builds requested link visibility and google pay configuration`() {
        val googlePayConfiguration = GooglePayConfiguration(GooglePayConfiguration.Environment.Test)
        val state = ExpressCheckoutElement.Configuration()
            .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never)
            .googlePayConfiguration(googlePayConfiguration)
            .build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Never
        )
        assertThat(state.googlePayConfiguration).isEqualTo(googlePayConfiguration.build())
    }

    @Test
    fun `configuration builds shipping address required`() {
        val state = ExpressCheckoutElement.Configuration()
            .shippingAddressRequired(true)
            .build()

        assertThat(state.shippingAddressRequired).isTrue()
    }
}
