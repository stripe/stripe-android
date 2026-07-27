package com.stripe.android.checkout

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration defaults payment method visibilities to auto`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.paymentMethods.link).isEqualTo(
            ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Auto
        )
        assertThat(state.paymentMethods.googlePay).isEqualTo(
            ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Auto
        )
        assertThat(state.shippingAddressRequired).isFalse()
        assertThat(state.allowedShippingCountries).isEmpty()
        assertThat(state.buttonHeight).isNull()
        assertThat(state.buttonOrientation).isEqualTo(
            ExpressCheckoutElement.Configuration.ButtonOrientation.Vertical
        )
    }

    @Test
    fun `configuration builds payment methods shipping and layout options`() {
        val state = ExpressCheckoutElement.Configuration()
            .paymentMethods(
                ExpressCheckoutElement.Configuration.PaymentMethods()
                    .link(ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Never)
                    .googlePay(ExpressCheckoutElement.Configuration.PaymentMethods.GooglePayVisibility.Auto)
            )
            .shippingAddressRequired(true)
            .allowedShippingCountries(setOf("ca", "US"))
            .buttonHeight(48.dp)
            .buttonOrientation(ExpressCheckoutElement.Configuration.ButtonOrientation.Horizontal)
            .build()

        assertThat(state.paymentMethods.link).isEqualTo(
            ExpressCheckoutElement.Configuration.PaymentMethods.LinkVisibility.Never
        )
        assertThat(state.shippingAddressRequired).isTrue()
        assertThat(state.allowedShippingCountries).containsExactly("CA", "US")
        assertThat(state.buttonHeight).isEqualTo(48f)
        assertThat(state.buttonOrientation).isEqualTo(
            ExpressCheckoutElement.Configuration.ButtonOrientation.Horizontal
        )
    }
}
