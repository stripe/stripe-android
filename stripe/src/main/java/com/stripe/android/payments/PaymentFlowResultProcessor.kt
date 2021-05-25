package com.stripe.android.payments

import com.stripe.android.model.StripeIntent

internal interface PaymentFlowResultProcessor<StripeIntentResult> {

    suspend fun processResult(
        unvalidatedResult: PaymentFlowResult.Unvalidated
    ): StripeIntentResult

    /**
     * It is very important to check `requiresAction()` because we can't always tell what
     * action the customer took during payment authentication (e.g. when using Custom Tabs).
     *
     * We don't want to cancel if required actions have been resolved and the payment is ready
     * for capture.
     */
    fun shouldCancelIntent(
        stripeIntent: StripeIntent,
        shouldCancelSource: Boolean
    ): Boolean {
        return shouldCancelSource && stripeIntent.requiresAction()
    }

    companion object {
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}
