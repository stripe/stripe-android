package com.stripe.android.financialconnections.launcher

import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheet

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FinancialConnectionsSheetLauncher {
    fun present(
        configuration: FinancialConnectionsSheet.Configuration,
        elementsSessionContext: FinancialConnectionsSheet.ElementsSessionContext? = null,
    )
}
