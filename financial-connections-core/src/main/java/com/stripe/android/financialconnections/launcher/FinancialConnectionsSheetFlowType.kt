package com.stripe.android.financialconnections.launcher

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class FinancialConnectionsSheetFlowType {
    ForData,
    ForInstantDebits,
    ForToken,
}
