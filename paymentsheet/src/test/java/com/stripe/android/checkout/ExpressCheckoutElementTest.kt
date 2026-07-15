package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration defaults both wallet visibilities to auto`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Auto
        )
        assertThat(state.googlePayVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayVisibility.Auto
        )
    }

    @Test
    fun `configuration builds requested wallet visibilities`() {
        val state = ExpressCheckoutElement.Configuration()
            .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never)
            .googlePayVisibility(ExpressCheckoutElement.Configuration.GooglePayVisibility.Never)
            .build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Never
        )
        assertThat(state.googlePayVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayVisibility.Never
        )
    }
}
