package com.stripe.android.payments

import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures

internal class FakePaymentFlowResultProcessor : PaymentFlowResultProcessor {
    var error: Throwable? = null

    var paymentIntentResult = PaymentIntentResult(
        PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
        StripeIntentResult.Outcome.FAILED
    )

    var setupIntentResult = SetupIntentResult(
        SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR,
        StripeIntentResult.Outcome.FAILED
    )

    override suspend fun processPaymentIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): PaymentIntentResult {
        return error?.let {
            throw it
        } ?: paymentIntentResult
    }

    override suspend fun processSetupIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): SetupIntentResult {
        return error?.let {
            throw it
        } ?: setupIntentResult
    }
}
