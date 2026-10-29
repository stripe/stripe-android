package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class CheckoutSessionResponseTest {
    @Test
    fun `extracts the session ID from a test client secret`() {
        assertThat(CheckoutSessionResponse.getIdFromClientSecret("cs_test_abc123_secret_value"))
            .isEqualTo("cs_test_abc123")
    }

    @Test
    fun `extracts the session ID from a live client secret`() {
        assertThat(CheckoutSessionResponse.getIdFromClientSecret("cs_live_abc123_secret_value"))
            .isEqualTo("cs_live_abc123")
    }

    @Test
    fun `omits session identity from malformed client secrets`(
        @TestParameter(
            value = [
                "",
                "not_a_client_secret",
                "cs_test_abc123",
                "cs_unknown_abc123_secret_value",
                "cs_test_abc123_secret_",
                "cs_test_abc123_secret_value_extra",
            ]
        ) clientSecret: String,
    ) {
        assertThat(CheckoutSessionResponse.getIdFromClientSecret(clientSecret)).isNull()
    }

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
