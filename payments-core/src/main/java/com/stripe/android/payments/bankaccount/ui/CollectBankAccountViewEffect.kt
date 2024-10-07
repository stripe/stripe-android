package com.stripe.android.payments.bankaccount.ui

import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal

/**
 * Used communicate view side-effects from [CollectBankAccountViewModel]
 * to [CollectBankAccountActivity] following the Unidirectional Data Flow conventions.
 *
 * Any one-off events should be communicated to the view via an instance of this sealed class.
 */
internal sealed class CollectBankAccountViewEffect {

    /**
     * Instruct the view to open the financial connections SDK flow.
     */
    data class OpenConnectionsFlow(
        val publishableKey: String,
        val financialConnectionsSessionSecret: String,
        val stripeAccountId: String?,
        val elementsSessionContext: ElementsSessionContext?,
    ) : CollectBankAccountViewEffect()

    /**
     * Instruct the view to finish with the given [CollectBankAccountResultInternal].
     */
    data class FinishWithResult(
        val result: CollectBankAccountResultInternal
    ) : CollectBankAccountViewEffect()
}
