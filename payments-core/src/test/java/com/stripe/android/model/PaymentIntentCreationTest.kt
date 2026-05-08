package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PaymentIntentCreationTest {

    @Test
    fun `PaymentIntent created with valid amount and currency reflects those values`() {
        val paymentIntent = PaymentIntent(
            id = "pi_test_123",
            paymentMethodTypes = listOf("card"),
            amount = 1000L,
            currency = "usd",
            countryCode = null,
            confirmationMethod = PaymentIntent.ConfirmationMethod.Automatic,
            created = 0L,
            isLiveMode = false,
            unactivatedPaymentMethods = emptyList(),
            clientSecret = "pi_test_123_secret_abc",
        )

        assertThat(paymentIntent.amount).isEqualTo(1000L)
        assertThat(paymentIntent.currency).isEqualTo("usd")
    }
}
