package com.stripe.android.payments.bankaccount.ui

import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult

internal sealed class CollectBankAccountViewEffect {
    data class OpenConnectionsFlow(
        val publishableKey: String,
        val linkedAccountSessionClientSecret: String
    ) : CollectBankAccountViewEffect()

    data class FinishWithResult(
        val result: CollectBankAccountResult
    ) : CollectBankAccountViewEffect()
}
