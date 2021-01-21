package com.stripe.android.payments

import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult

internal interface PaymentFlowResultProcessor {
    suspend fun processPaymentIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): PaymentIntentResult

    suspend fun processSetupIntent(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): SetupIntentResult
}
