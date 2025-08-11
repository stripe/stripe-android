package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountTextBuilder
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSelectionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Doesn't display a mandate for Link`() {
        val link = PaymentSelection.Link()
        val result = link.mandateText(
            merchantName = "Merchant",
            isSetupFlow = false,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Link billingDetails returns null when selectedPayment is null`() {
        val link = PaymentSelection.Link(selectedPayment = null)

        assertThat(link.billingDetails).isNull()
    }

    @Test
    fun `Link billingDetails returns complete billing details when selectedPayment has full billing info`() {
        val billingAddress = ConsumerPaymentDetails.BillingAddress(
            name = "John Doe",
            line1 = "123 Main St",
            line2 = "Apt 4B",
            administrativeArea = "CA",
            locality = "San Francisco",
            postalCode = "94111",
            countryCode = CountryCode.US,
        )

        val paymentDetails = ConsumerPaymentDetails.Card(
            id = "pm_123",
            last4 = "4242",
            isDefault = true,
            nickname = "My Card",
            billingAddress = billingAddress,
            billingEmailAddress = "john@example.com",
            expiryYear = 2025,
            expiryMonth = 12,
            brand = CardBrand.Visa,
            networks = listOf("visa"),
            cvcCheck = CvcCheck.Pass,
            funding = "credit"
        )

        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = paymentDetails,
            collectedCvc = null,
            billingPhone = "+1-555-123-4567"
        )

        val link = PaymentSelection.Link(selectedPayment = selectedPayment)

        assertThat(link.billingDetails).isEqualTo(
            PaymentMethod.BillingDetails(
                address = Address(
                    city = "San Francisco",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4B",
                    postalCode = "94111",
                    state = "CA",
                ),
                email = "john@example.com",
                phone = "+1-555-123-4567",
                name = "John Doe",
            )
        )
    }

    @Test
    fun `Doesn't display a mandate for Google Pay`() {
        val googlePay = PaymentSelection.GooglePay
        val result = googlePay.mandateText(
            merchantName = "Merchant",
            isSetupFlow = false,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `Displays the correct mandate for a saved US bank account when not saving for future use`() {
        val newPaymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )

        val result = newPaymentSelection.mandateText(
            merchantName = "Merchant",
            isSetupFlow = false,
        )?.resolve(context)

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
            merchantName = "Merchant",
            isSetupFlow = false,
        )?.resolve(context)

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
            merchantName = "Merchant",
            isSetupFlow = false,
        )?.resolve(context)

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
            merchantName = "Merchant",
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

    @Test
    fun `mandateTextFromPaymentMethodMetadata returns correct mandate for USBankAccount with PMO SFU set`() {
        val selection = PaymentSelection.Saved(
            PaymentMethodFactory.usBankAccount()
        )
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = """
                    {
                        "us_bank_account": {
                            "setup_future_usage": "off_session"
                        }
                    }
                """.trimIndent()
            )
        )
        assertThat(
            selection.mandateTextFromPaymentMethodMetadata(metadata)
        ).isEqualTo(
            USBankAccountTextBuilder.buildMandateText(
                merchantName = metadata.merchantName,
                isSaveForFutureUseSelected = false,
                isInstantDebits = false,
                isSetupFlow = true
            )
        )
    }
}
