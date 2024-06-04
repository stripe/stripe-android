package com.stripe.android.financialconnections.example

import com.stripe.android.financialconnections.FinancialConnectionsSheet

sealed class FinancialConnectionsExampleViewEffect {
    data class OpenFinancialConnectionsSheetExample(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsExampleViewEffect()
}
