package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsFinancialConnectionsSdkAvailable {
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultIsFinancialConnectionsAvailable : IsFinancialConnectionsSdkAvailable {
    override operator fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.financialconnections.FinancialConnectionsSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
