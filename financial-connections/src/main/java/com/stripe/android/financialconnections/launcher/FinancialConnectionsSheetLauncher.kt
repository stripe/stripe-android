package com.stripe.android.financialconnections.launcher

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import com.stripe.android.financialconnections.FinancialConnectionsSheet

@RestrictTo(LIBRARY_GROUP) interface FinancialConnectionsSheetLauncher {
    fun present(configuration: FinancialConnectionsSheet.Configuration)
}
