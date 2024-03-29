package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsFinancialConnectionsAvailable {
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultIsFinancialConnectionsAvailable : IsFinancialConnectionsAvailable {
    override operator fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.financialconnections.FinancialConnectionsSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
