package com.stripe.android.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
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
            CheckoutGooglePayConfiguration.Display.Automatic
        )
        assertThat(state.googlePayConfiguration.label).isNull()
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            PaymentSheet.GooglePayConfiguration.ButtonType.Pay
        )
        assertThat(state.googlePayConfiguration.additionalEnabledNetworks).isEmpty()
        assertThat(state.paymentMethodOrder).isEmpty()
        assertThat(state.appearance.buttonLayout.maxColumns).isNull()
        assertThat(state.appearance.buttonLayout.maxRows).isNull()
        assertThat(state.appearance.buttonTheme).isEqualTo(
            ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic
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
            CheckoutGooglePayConfiguration.Display.Never
        )
        assertThat(state.googlePayConfiguration.label).isEqualTo("Complete your purchase")
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
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
    fun `configuration builds payment method order`() {
        val state = ExpressCheckoutElement.Configuration()
            .paymentMethodOrder(
                listOf(
                    "link",
                    "google_pay",
                )
            )
            .build()

        assertThat(state.paymentMethodOrder).containsExactly(
            ExpressCheckoutElement.Configuration.PaymentMethodType.Link,
            ExpressCheckoutElement.Configuration.PaymentMethodType.GooglePay,
        ).inOrder()
    }

    @Test
    fun `configuration ignores invalid payment methods in payment method order`() {
        val state = ExpressCheckoutElement.Configuration()
            .paymentMethodOrder(listOf("invalid", "link"))
            .build()

        assertThat(state.paymentMethodOrder).containsExactly(
            ExpressCheckoutElement.Configuration.PaymentMethodType.Link,
        )
    }

    @Test
    fun `configuration builds requested appearance`() {
        val state = ExpressCheckoutElement.Configuration()
            .appearance(
                ExpressCheckoutElement.Configuration.Appearance()
                    .buttonLayout(
                        ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                            .maxColumns(2)
                            .maxRows(1)
                    )
                    .buttonTheme(ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light)
            )
            .build()

        assertThat(state.appearance.buttonLayout.maxColumns).isEqualTo(2)
        assertThat(state.appearance.buttonLayout.maxRows).isEqualTo(1)
        assertThat(state.appearance.buttonTheme).isEqualTo(
            ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light
        )
    }

    @Test
    fun `button layout rejects non-positive maximum columns`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxColumns(0)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `button layout rejects non-positive maximum rows`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxRows(-1)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
