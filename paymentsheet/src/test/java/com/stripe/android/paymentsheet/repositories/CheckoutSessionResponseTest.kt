package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import org.junit.Test

internal class CheckoutSessionResponseTest {
    @Test
    fun `processing payment completes the session without claiming it is paid`() {
        val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(status = StripeIntent.Status.Processing)

        val response = CheckoutSessionResponseFactory.create().withConfirmedIntent(intent)

        assertThat(response.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(response.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.UNPAID)
        assertThat(response.paymentIntent).isEqualTo(intent)
    }

    @Test
    fun `successful setup completes a no payment required session`() {
        val intent = SetupIntentFixtures.SI_SUCCEEDED
        val response = CheckoutSessionResponseFactory.create(
            paymentStatus = CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED,
        ).withConfirmedIntent(intent)

        assertThat(response.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(response.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED)
        assertThat(response.setupIntent).isEqualTo(intent)
    }

    @Test
    fun `unfinished authentication leaves the session open`() {
        val response = CheckoutSessionResponseFactory.create()
            .withConfirmedIntent(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)

        assertThat(response.status).isEqualTo(CheckoutSessionResponse.Status.OPEN)
        assertThat(response.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.UNPAID)
    }
}
