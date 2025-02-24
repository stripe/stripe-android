package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.Amount
import org.junit.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

class PrimaryButtonUtilsTest {
    @Test
    fun `buyButtonLabel returns primaryButtonLabel if provided`() {
        val label = buyButtonLabel(
            amount = null,
            primaryButtonLabel = "Test Label",
            isForPaymentIntent = true
        )

        assertThat(label).isEqualTo("Test Label".resolvableString)
    }

    @Test
    fun `buyButtonLabel returns resolvableString with correct args`() {
        val label = buyButtonLabel(
            amount = Amount(
                value = 1099,
                currencyCode = "usd"
            ),
            primaryButtonLabel = null,
            isForPaymentIntent = true
        )

        assertThat(label).isEqualTo(
            resolvableString(
                id = StripeUiCoreR.string.stripe_pay_button_amount,
                formatArgs = arrayOf("$10.99")
            )
        )
    }

    @Test
    fun `buyButtonLabel returns Set up if isForPaymentIntent is false`() {
        val label = buyButtonLabel(
            amount = null,
            primaryButtonLabel = null,
            isForPaymentIntent = false
        )

        assertThat(label).isEqualTo(StripeUiCoreR.string.stripe_setup_button_label.resolvableString)
    }

    @Test
    fun `buyButtonLabel returns fallback if amount is null`() {
        val label = buyButtonLabel(
            amount = null,
            primaryButtonLabel = null,
            isForPaymentIntent = true
        )

        assertThat(label).isEqualTo(R.string.stripe_paymentsheet_pay_button_label.resolvableString)
    }

    @Test
    fun `continueButtonLabel returns primaryButtonLabel if not null`() {
        val label = continueButtonLabel(
            primaryButtonLabel = "Test Label",
        )

        assertThat(label).isEqualTo("Test Label".resolvableString)
    }

    @Test
    fun `continueButtonLabel returns Continue if primaryButtonLabel is null`() {
        val label = continueButtonLabel(null)

        assertThat(label).isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
    }
}
