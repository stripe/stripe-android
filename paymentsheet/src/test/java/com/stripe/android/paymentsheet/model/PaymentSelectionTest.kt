package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Doesn't display a mandate for Link`() =
        runAllConfigurations { isSaveForFutureUseSelected ->
            val link = PaymentSelection.Link
            val result = link.mandateText(
                context = context,
                merchantName = "Merchant",
                isSaveForFutureUseSelected = isSaveForFutureUseSelected,
                isSetupFlow = false,
            )
            assertThat(result).isNull()
        }

    @Test
    fun `Doesn't display a mandate for Google Pay`() =
        runAllConfigurations { isSaveForFutureUseSelected ->
            val googlePay = PaymentSelection.GooglePay
            val result = googlePay.mandateText(
                context = context,
                merchantName = "Merchant",
                isSaveForFutureUseSelected = isSaveForFutureUseSelected,
                isSetupFlow = false,
            )
            assertThat(result).isNull()
        }

    @Test
    fun `Doesn't display a mandate for new US bank accounts`() =
        runAllConfigurations { isSaveForFutureUseSelected ->
            // We actually do show a mandate, but it's set independently from the PaymentSelection.
            val newPaymentSelection = PaymentSelection.New.USBankAccount(
                labelResource = "Test",
                iconResource = 0,
                paymentMethodCreateParams = mock(),
                customerRequestedSave = mock(),
                input = PaymentSelection.New.USBankAccount.Input(
                    name = "",
                    email = null,
                    phone = null,
                    address = null,
                    saveForFutureUse = false,
                ),
                screenState = USBankAccountFormScreenState.SavedAccount(
                    financialConnectionsSessionId = "session_1234",
                    intentId = "intent_1234",
                    bankName = "Stripe Bank",
                    last4 = "6789",
                    primaryButtonText = "Continue",
                    mandateText = null,
                ),
            )

            val result = newPaymentSelection.mandateText(
                context = context,
                merchantName = "Merchant",
                isSaveForFutureUseSelected = isSaveForFutureUseSelected,
                isSetupFlow = false,
            )

            assertThat(result).isNull()
        }

    @Test
    fun `Displays the correct mandate for a saved US bank account when saving for future use`() {
        val newPaymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )

        var result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = true,
            isSetupFlow = false,
        )

        assertThat(result).isEqualTo(
            "By saving your bank account for Merchant you agree to authorize payments pursuant " +
                "to <a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
        )

        result = newPaymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = false,
            isSetupFlow = true,
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
            isSetupFlow = false,
        )

        assertThat(result).isEqualTo(
            "By continuing, you agree to authorize payments pursuant to " +
                "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
        )
    }

    @Test
    fun `Displays the correct mandate for a sepa family PMs`() {
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.sepaDebit(),
        )

        val result = paymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
        )

        assertThat(result).isEqualTo(
            "By providing your payment information and confirming this payment, you authorise (A) Merchant and" +
                " Stripe, our payment service provider, to send instructions to your bank to debit your account and" +
                " (B) your bank to debit your account in accordance with those instructions. As part of your rights," +
                " you are entitled to a refund from your bank under the terms and conditions of your agreement with" +
                " your bank. A refund must be claimed within 8 weeks starting from the date on which your account " +
                "was debited. Your rights are explained in a statement that you can obtain from your bank. You agree" +
                " to receive notifications for future debits up to 2 days before they occur."
        )
    }

    @Test
    fun `Displays the correct mandate for US Bank Account`() {
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )

        val result = paymentSelection.mandateText(
            context = context,
            merchantName = "Merchant",
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
        )

        assertThat(result).isEqualTo(
            "By continuing, you agree to authorize payments pursuant to " +
                "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
        )
    }

    @Test
    fun `Doesn't display a mandate for a saved payment method that isn't US bank account`() =
        runAllConfigurations { isSaveForFutureUseSelected ->
            val newPaymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.cashAppPay(),
            )

            val result = newPaymentSelection.mandateText(
                context = context,
                merchantName = "Merchant",
                isSaveForFutureUseSelected = isSaveForFutureUseSelected,
                isSetupFlow = false,
            )
            assertThat(result).isNull()
        }

    @Test
    fun `showMandateAbovePrimaryButton is true for sepa family`() {
        assertThat(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.sepaDebit(),
            ).showMandateAbovePrimaryButton
        ).isTrue()
    }

    @Test
    fun `showMandateAbovePrimaryButton is false for US Bank Account`() {
        assertThat(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.usBankAccount(),
            ).showMandateAbovePrimaryButton
        ).isFalse()
    }

    @Test
    fun `requiresConfirmation is true for US Bank Account and Sepa Family`() {
        assertThat(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.usBankAccount(),
            ).requiresConfirmation
        ).isTrue()
        assertThat(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.sepaDebit(),
            ).requiresConfirmation
        ).isTrue()
    }

    @Test
    fun `requiresConfirmation is false for cards`() {
        assertThat(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.card(),
            ).requiresConfirmation
        ).isFalse()
    }

    private fun runAllConfigurations(block: (Boolean) -> Unit) {
        for (isSaveForFutureUseSelected in listOf(true, false)) {
            block(isSaveForFutureUseSelected)
        }
    }
}
