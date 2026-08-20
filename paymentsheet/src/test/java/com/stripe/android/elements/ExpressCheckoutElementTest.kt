package com.stripe.android.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(
    CheckoutSessionPreview::class,
    CollectMissingLinkBillingDetailsPreview::class,
    LinkDisallowFundingSourceCreationPreview::class,
)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration builds default values`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
        )
        assertThat(state.linkConfiguration.collectMissingBillingDetailsForExistingPaymentMethods).isTrue()
        assertThat(state.linkConfiguration.disallowFundingSourceCreation).isEmpty()
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
            .linkConfiguration(
                ExpressCheckoutElement.Configuration.LinkConfiguration()
                    .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                    .collectMissingBillingDetailsForExistingPaymentMethods(false)
                    .disallowFundingSourceCreation(setOf("card", "bank_account"))
            )
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
                    .label("Complete your purchase")
                    .buttonType(ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("INTERAC"))
            )
            .build()

        assertThat(state.linkConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
        )
        assertThat(state.linkConfiguration.collectMissingBillingDetailsForExistingPaymentMethods).isFalse()
        assertThat(state.linkConfiguration.disallowFundingSourceCreation)
            .containsExactly("card", "bank_account")
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
