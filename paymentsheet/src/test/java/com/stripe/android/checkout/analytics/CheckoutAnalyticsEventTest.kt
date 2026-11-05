package com.stripe.android.checkout.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import kotlin.test.Test

class CheckoutAnalyticsEventTest {
    @Test
    fun `event name omits the outcome suffix when there is no outcome`() {
        assertThat(event(Operation.ClearPaymentOption, outcome = null).eventName)
            .isEqualTo("checkout_elements.clear_payment_option")
    }

    @Test
    fun `event name appends the outcome code`() {
        assertThat(event(Operation.UpdateEmail, Outcome.Succeeded).eventName)
            .isEqualTo("checkout_elements.update_email.succeeded")
    }

    private fun event(operation: Operation, outcome: Outcome?) = CheckoutAnalyticsEvent(
        operation = operation,
        outcome = outcome,
        analyticsMetadata = AnalyticsMetadata(emptyMap()),
        duration = null,
        error = null,
        operationParams = emptyMap(),
    )
}
