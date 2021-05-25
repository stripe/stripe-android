package com.stripe.android.payments

import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntentFixtures

internal class FakePaymentIntentFlowResultProcessor :
    PaymentFlowResultProcessor<PaymentIntentResult> {
    var error: Throwable? = null

    var paymentIntentResult = PaymentIntentResult(
        PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
        StripeIntentResult.Outcome.FAILED
    )

    override suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): PaymentIntentResult {
        return error?.let {
            throw it
        } ?: paymentIntentResult
    }
}
