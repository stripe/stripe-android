package com.stripe.android.payments

import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.SetupIntentFixtures

internal class FakeSetupIntentFlowResultProcessor :
    PaymentFlowResultProcessor<SetupIntentResult> {
    var error: Throwable? = null

    var setupIntentResult = SetupIntentResult(
        SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR,
        StripeIntentResult.Outcome.FAILED
    )

    override suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): SetupIntentResult {
        return error?.let {
            throw it
        } ?: setupIntentResult
    }
}
