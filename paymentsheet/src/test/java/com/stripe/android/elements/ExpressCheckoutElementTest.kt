package com.stripe.android.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration builds default values`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Auto
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic
        )
        assertThat(state.googlePayConfiguration.label).isNull()
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Pay
        )
        assertThat(state.googlePayConfiguration.additionalEnabledNetworks).isEmpty()
        assertThat(state.billingDetailsCollectionConfiguration.name).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        )
        assertThat(state.billingDetailsCollectionConfiguration.email).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        )
        assertThat(state.billingDetailsCollectionConfiguration.address).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        )
    }

    @Test
    fun `configuration builds requested values`() {
        val state = ExpressCheckoutElement.Configuration()
            .linkVisibility(ExpressCheckoutElement.Configuration.LinkVisibility.Never)
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
                    .label("Complete your purchase")
                    .buttonType(ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("INTERAC"))
            )
            .build()

        assertThat(state.linkVisibility).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkVisibility.Never
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never
        )
        assertThat(state.googlePayConfiguration.label).isEqualTo("Complete your purchase")
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout
        )
        assertThat(state.googlePayConfiguration.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `configuration builds shipping address required`() {
        val state = ExpressCheckoutElement.Configuration()
            .shippingAddressRequired(true)
            .build()

        assertThat(state.shippingAddressRequired).isTrue()
    }

    @Test
    fun `configuration builds requested billing details collection values`() {
        val state = ExpressCheckoutElement.Configuration()
            .billingDetailsCollectionConfiguration(
                ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration()
                    .name(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Always
                    )
                    .email(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Never
                    )
                    .address(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration
                            .AddressCollectionMode.Full
                    )
            )
            .build()

        assertThat(state.billingDetailsCollectionConfiguration.name).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Always
        )
        assertThat(state.billingDetailsCollectionConfiguration.email).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Never
        )
        assertThat(state.billingDetailsCollectionConfiguration.address).isEqualTo(
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        )
    }
}
