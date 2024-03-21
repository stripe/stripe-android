package com.stripe.android.financialconnections.presentation

internal sealed interface FinancialConnectionsErrorAction {
    data class Close(val error: Throwable) : FinancialConnectionsErrorAction
    data object EnterDetailsManually : FinancialConnectionsErrorAction
    data object SelectAnotherBank : FinancialConnectionsErrorAction
    data object TryAgain : FinancialConnectionsErrorAction
}
