package com.stripe.android.payments

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.StripeException
import kotlin.test.Test

class PaymentFlowResultTest {

    @Test
    fun `validate() should fail if clientSecret is empty`() {
        val result = PaymentFlowResult.Unvalidated(
            clientSecret = "   ",
        ).validate()

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Invalid client_secret value in result Intent.")
    }

    @Test
    fun `validate() should fail if exception is non-null`() {
        val result = PaymentFlowResult.Unvalidated(
            exception = StripeException.create(RuntimeException("failure")),
        ).validate()

        assertThat(result.exceptionOrNull()?.message).isEqualTo("failure")
    }

    @Test
    fun `validate() should return Validated instance if valid`() {
        val result = PaymentFlowResult.Unvalidated(
            clientSecret = "client_secret"
        ).validate()

        assertThat(result.getOrNull()).isEqualTo(
            PaymentFlowResult.Validated(
                clientSecret = "client_secret",
                flowOutcome = 0
            )
        )
    }
}
