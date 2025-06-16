package com.stripe.android.link.ui.wallet

import com.stripe.android.financialconnections.FinancialConnectionsSheet

internal sealed interface AddBankAccountState {
    data object Idle : AddBankAccountState

    data object ConfiguringFinancialConnections : AddBankAccountState

    data class FinancialConnectionsConfigured(
        val config: FinancialConnectionsSheet.Configuration
    ) : AddBankAccountState

    data object PresentingFinancialConnections : AddBankAccountState

    data object ProcessingFinancialConnectionsResult : AddBankAccountState
}
