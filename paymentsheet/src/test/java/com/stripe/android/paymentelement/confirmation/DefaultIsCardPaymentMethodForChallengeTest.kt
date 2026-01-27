package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test

internal class DefaultIsCardPaymentMethodForChallengeTest {

    private val isCardPaymentMethod = DefaultIsCardPaymentMethodForChallenge

    @Test
    fun `returns true for new card payment method`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(isCardPaymentMethod(option)).isTrue()
    }

    @Test
    fun `returns false for new non-card payment method`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.PAYPAL,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(isCardPaymentMethod(option)).isFalse()
    }

    @Test
    fun `returns true for new Link payment method with card as original payment method`() {
        val linkParams = PaymentMethodCreateParams.createLink(
            paymentDetailsId = "payment_details_id",
            consumerSessionClientSecret = "consumer_secret",
            clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            originalPaymentMethodCode = "card",
        )
        val option = PaymentMethodConfirmationOption.New(
            createParams = linkParams,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(isCardPaymentMethod(option)).isTrue()
    }

    @Test
    fun `returns false for new Link payment method without original payment method code`() {
        val linkParams = PaymentMethodCreateParams.createLink(
            paymentDetailsId = "payment_details_id",
            consumerSessionClientSecret = "consumer_secret",
            clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            originalPaymentMethodCode = null,
        )
        val option = PaymentMethodConfirmationOption.New(
            createParams = linkParams,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(isCardPaymentMethod(option)).isFalse()
    }

    @Test
    fun `returns false for new Link payment method with non-card original payment method`() {
        val linkParams = PaymentMethodCreateParams.createLink(
            paymentDetailsId = "payment_details_id",
            consumerSessionClientSecret = "consumer_secret",
            clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            originalPaymentMethodCode = "us_bank_account",
        )
        val option = PaymentMethodConfirmationOption.New(
            createParams = linkParams,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(isCardPaymentMethod(option)).isFalse()
    }

    @Test
    fun `returns true for saved card payment method`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        assertThat(isCardPaymentMethod(option)).isTrue()
    }

    @Test
    fun `returns false for saved non-card payment method`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            optionsParams = null,
        )

        assertThat(isCardPaymentMethod(option)).isFalse()
    }

    @Test
    fun `returns true for saved Link payment method with card details`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.LINK_PAYMENT_METHOD,
            optionsParams = null,
        )

        assertThat(isCardPaymentMethod(option)).isTrue()
    }

    @Test
    fun `returns true for saved payment method in Link passthrough mode`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                isLinkPassthroughMode = true
            ),
            optionsParams = null,
        )

        assertThat(isCardPaymentMethod(option)).isTrue()
    }

    @Test
    fun `returns false for saved Link payment method with bank account details`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.copy(
                linkPaymentDetails = LinkPaymentDetails.BankAccount(
                    last4 = "4242",
                    bankName = "Stripe Bank",
                )
            ),
            optionsParams = null,
        )

        assertThat(isCardPaymentMethod(option)).isFalse()
    }
}
