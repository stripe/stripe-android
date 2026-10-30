package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class CheckoutSessionResponseTest {
    @Test
    fun `poll response replaces session and payment status`() {
        val original = CheckoutSessionResponseFactory.create()

        val response = original.withPollResponse(
            CheckoutClientPollResponse(
                id = original.id,
                confirmationStatus = CheckoutClientPollResponse.ConfirmationStatus.SUCCEEDED,
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )

        assertThat(response).isEqualTo(
            original.copy(
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )
    }

    @Test
    fun `missing poll response keeps the response`() {
        val original = CheckoutSessionResponseFactory.create()

        assertThat(original.withPollResponse(null)).isSameInstanceAs(original)
    }
}
