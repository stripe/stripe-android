package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentIntentResultTest {

    @Test
    fun `intent should return expected object`() {
        assertThat(PaymentIntentResult(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2).intent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
    }

    @Test
    fun outcome_whenBacsAndProcessing_shouldReturnSucceeded() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.BACS_DEBIT_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            paymentMethodTypes = listOf("card"),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun outcome_whenCardAndProcessing_shouldReturnUnknown() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            paymentMethodTypes = listOf("card"),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `should parcelize correctly`() {
        ParcelUtils.verifyParcelRoundtrip(
            PaymentIntentResult(
                intent = PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
                outcomeFromFlow = StripeIntentResult.Outcome.CANCELED
            )
        )
    }
}
