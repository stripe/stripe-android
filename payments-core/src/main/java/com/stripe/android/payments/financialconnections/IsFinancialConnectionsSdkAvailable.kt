package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
<<<<<<<< HEAD:payments-core/src/main/java/com/stripe/android/payments/financialconnections/IsFinancialConnectionsFullSdkAvailable.kt
fun interface IsFinancialConnectionsFullSdkAvailable {
========
fun interface IsFinancialConnectionsSdkAvailable {
>>>>>>>> origin/master:payments-core/src/main/java/com/stripe/android/payments/financialconnections/IsFinancialConnectionsSdkAvailable.kt
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
<<<<<<<< HEAD:payments-core/src/main/java/com/stripe/android/payments/financialconnections/IsFinancialConnectionsFullSdkAvailable.kt
object DefaultIsFinancialConnectionsAvailable : IsFinancialConnectionsFullSdkAvailable {
========
object DefaultIsFinancialConnectionsAvailable : IsFinancialConnectionsSdkAvailable {
>>>>>>>> origin/master:payments-core/src/main/java/com/stripe/android/payments/financialconnections/IsFinancialConnectionsSdkAvailable.kt
    override operator fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.financialconnections.FinancialConnectionsSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
