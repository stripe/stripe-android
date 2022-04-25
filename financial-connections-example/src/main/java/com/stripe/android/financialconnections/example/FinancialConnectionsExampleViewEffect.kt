package com.stripe.android.financialconnections.example

import com.stripe.android.financialconnections.FinancialConnectionsSheet

sealed class FinancialConnectionsExampleViewEffect {
    data class OpenConnectionsSheetExample(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsExampleViewEffect()
}