package com.stripe.android.payments

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.StripeException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PaymentFlowResultTest {

    @Test
    fun `validate() should fail if clientSecret is empty`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            PaymentFlowResult.Unvalidated(
                clientSecret = "   "
            ).validate()
        }
        assertThat(exception.message)
            .isEqualTo("Invalid client_secret value in result Intent.")
    }

    @Test
    fun `validate() should fail if exception is non-null`() {
        val exception = assertFailsWith<APIException> {
            PaymentFlowResult.Unvalidated(
                exception = StripeException.create(RuntimeException("failure"))
            ).validate()
        }
        assertThat(exception.message)
            .isEqualTo("failure")
    }

    @Test
    fun `validate() should return Validated instance if valid`() {
        assertThat(
            PaymentFlowResult.Unvalidated(
                clientSecret = "client_secret"
            ).validate()
        ).isEqualTo(
            PaymentFlowResult.Validated(
                clientSecret = "client_secret",
                flowOutcome = 0
            )
        )
    }
}
