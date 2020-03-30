package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentResultTest {

    @Test
    fun testBuilder() {
        assertThat(PaymentIntentResult(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2).intent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
    }

    @Test
    fun outcome_whenBacsAndProcessing_shouldReturnSucceeded() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            captureMethod = "automatic",
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.BACS_DEBIT_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            objectType = "payment_intent",
            paymentMethodTypes = listOf("card"),
            status = StripeIntent.Status.Processing
        )
        val result = PaymentIntentResult(
            paymentIntent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }
}
