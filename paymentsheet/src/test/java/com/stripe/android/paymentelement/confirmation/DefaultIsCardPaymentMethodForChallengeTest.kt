package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test

internal class DefaultIsCardPaymentMethodForChallengeTest {

    private val isCardPaymentMethod = DefaultIsCardPaymentMethodForChallenge

    @Test
    fun `returns true for new card payment method`() {
        val actual = isCardPaymentMethod(newOption(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
        assertThat(actual).isTrue()
    }

    @Test
    fun `returns false for new non-card payment method`() {
        val actual = isCardPaymentMethod(newOption(PaymentMethodCreateParamsFixtures.PAYPAL))
        assertThat(actual).isFalse()
    }

    @Test
    fun `returns true for new Link payment method with card as original payment method`() {
        val actual = isCardPaymentMethod(newOption(linkParams(originalPaymentMethodCode = "card")))
        assertThat(actual).isTrue()
    }

    @Test
    fun `returns false for new Link payment method without original payment method code`() {
        val actual = isCardPaymentMethod(newOption(linkParams(originalPaymentMethodCode = null)))
        assertThat(actual).isFalse()
    }

    @Test
    fun `returns false for new Link payment method with non-card original payment method`() {
        val actual = isCardPaymentMethod(newOption(linkParams(originalPaymentMethodCode = "us_bank_account")))
        assertThat(actual).isFalse()
    }

    @Test
    fun `returns true for saved card payment method`() {
        val actual = isCardPaymentMethod(savedOption(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        assertThat(actual).isTrue()
    }

    @Test
    fun `returns false for saved non-card payment method`() {
        val actual = isCardPaymentMethod(savedOption(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD))
        assertThat(actual).isFalse()
    }

    @Test
    fun `returns true for saved Link payment method with card details`() {
        val actual = isCardPaymentMethod(savedOption(PaymentMethodFixtures.LINK_PAYMENT_METHOD))
        assertThat(actual).isTrue()
    }

    @Test
    fun `returns true for saved payment method in Link passthrough mode`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(isLinkPassthroughMode = true)
        val actual = isCardPaymentMethod(savedOption(paymentMethod))
        assertThat(actual).isTrue()
    }

    @Test
    fun `returns false for saved Link payment method with bank account details`() {
        val paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.copy(
            linkPaymentDetails = LinkPaymentDetails.BankAccount(last4 = "4242", bankName = "Stripe Bank")
        )
        val actual = isCardPaymentMethod(savedOption(paymentMethod))
        assertThat(actual).isFalse()
    }

    private fun newOption(createParams: PaymentMethodCreateParams) = PaymentMethodConfirmationOption.New(
        createParams = createParams,
        optionsParams = null,
        extraParams = null,
        shouldSave = false,
    )

    private fun savedOption(paymentMethod: PaymentMethod) = PaymentMethodConfirmationOption.Saved(
        paymentMethod = paymentMethod,
        optionsParams = null,
    )

    private fun linkParams(originalPaymentMethodCode: String?) = PaymentMethodCreateParams.createLink(
        paymentDetailsId = "payment_details_id",
        consumerSessionClientSecret = "consumer_secret",
        clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
        originalPaymentMethodCode = originalPaymentMethodCode,
    )
}
