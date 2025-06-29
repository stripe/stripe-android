package com.stripe.android.payments.financialconnections

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.intentBuilder
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.lite.intentBuilder as liteIntentBuilder

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class FinancialConnectionsAvailability {
    Full,
    Lite
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FinancialConnectionsAvailability.getIntentBuilder(
    context: Context
): (FinancialConnectionsSheetActivityArgs) -> Intent {
    return when (this) {
        FinancialConnectionsAvailability.Full -> intentBuilder(context)
        FinancialConnectionsAvailability.Lite -> liteIntentBuilder(context)
    }
}
