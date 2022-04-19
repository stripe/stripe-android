package com.stripe.android.financialconnections

internal interface FinancialConnectionsSheetLauncher {
    fun present(configuration: FinancialConnectionsSheet.Configuration)
}
