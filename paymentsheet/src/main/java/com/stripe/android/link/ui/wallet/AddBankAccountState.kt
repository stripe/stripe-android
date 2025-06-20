package com.stripe.android.link.ui.wallet

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration

internal sealed interface AddBankAccountState {
    data object Idle : AddBankAccountState

    data class Processing(val configToPresent: FinancialConnectionsSheetConfiguration? = null) : AddBankAccountState
}
