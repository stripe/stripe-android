package com.stripe.android.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration defaults both wallet visibilities to automatic`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Auto
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic
        )
    }

    @Test
    fun `configuration builds requested wallet visibilities`() {
        val state = ExpressCheckoutElement.Configuration()
            .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never)
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
            )
            .build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Never
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never
        )
    }

    @Test
    fun `configuration builds shipping address required`() {
        val state = ExpressCheckoutElement.Configuration()
            .shippingAddressRequired(true)
            .build()

        assertThat(state.shippingAddressRequired).isTrue()
    }
}
