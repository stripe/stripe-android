package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Doesn't display a mandate for Link`() {
        val link = PaymentSelection.Link
        val result = link.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = listOf(true, false).random(),
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Doesn't display a mandate for Google Pay`() {
        val googlePay = PaymentSelection.GooglePay
        val result = googlePay.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = listOf(true, false).random(),
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Doesn't display a mandate for new US bank accounts`() {
        // We actually do show a mandate, but it's set independently from the PaymentSelection.
        val newPaymentSelection = PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            bankName = "Test",
            last4 = "Test",
            financialConnectionsSessionId = "1234",
            intentId = "1234",
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
        )

        val result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = listOf(true, false).random(),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `Displays the correct mandate for a saved US bank account when saving for future use`() {
        val newPaymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )

        val result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = true,
        )

        assertThat(result).isEqualTo(
            "By saving your bank account for Merchant you agree to authorize payments pursuant " +
                "to <a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
        )
    }

    @Test
    fun `Displays the correct mandate for a saved US bank account when not saving for future use`() {
        val newPaymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )

        val result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = false,
        )

        assertThat(result).isEqualTo(
            "By continuing, you agree to authorize payments pursuant to " +
                "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
        )
    }

    @Test
    fun `Doesn't display a mandate for a saved payment method that isn't US bank account`() {
        val newPaymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.cashAppPay(),
        )

        val result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = listOf(true, false).random(),
        )
        assertThat(result).isNull()
    }
}
