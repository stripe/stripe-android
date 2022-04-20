package com.stripe.android.payments.financialconnections

internal interface IsFinancialConnectionsAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsFinancialConnectionsAvailable : IsFinancialConnectionsAvailable {
    override fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.financialconnections.FinancialConnectionsSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
