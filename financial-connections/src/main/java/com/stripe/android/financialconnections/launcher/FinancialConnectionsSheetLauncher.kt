package com.stripe.android.financialconnections.launcher

import com.stripe.android.financialconnections.FinancialConnectionsSheet

internal interface FinancialConnectionsSheetLauncher {
    fun present(configuration: FinancialConnectionsSheet.Configuration)
}
