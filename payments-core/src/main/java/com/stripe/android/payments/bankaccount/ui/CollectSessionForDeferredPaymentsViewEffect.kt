package com.stripe.android.payments.bankaccount.ui

import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsResult

/**
 * Used communicate view side-effects from [CollectSessionForDeferredPaymentsViewModel]
 * to [CollectSessionForDeferredPaymentsActivity] following the Unidirectional Data Flow conventions.
 *
 * Any one-off events should be communicated to the view via an instance of this sealed class.
 */
internal sealed class CollectSessionForDeferredPaymentsViewEffect {

    /**
     * Instruct the view to open the financial connections SDK flow.
     */
    data class OpenConnectionsFlow(
        val publishableKey: String,
        val financialConnectionsSessionSecret: String,
        val stripeAccountId: String?
    ) : CollectSessionForDeferredPaymentsViewEffect()

    /**
     * Instruct the view to finish with the given [CollectSessionForDeferredPaymentsResult].
     */
    data class FinishWithResult(
        val result: CollectSessionForDeferredPaymentsResult
    ) : CollectSessionForDeferredPaymentsViewEffect()
}
