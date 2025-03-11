package com.stripe.android.financialconnections.launcher

import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FinancialConnectionsSheetLauncher {
    fun present(
        configuration: FinancialConnectionsSheetConfiguration,
        elementsSessionContext: ElementsSessionContext? = null,
    )
}
