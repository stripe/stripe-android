package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class FinancialConnectionsAvailability(val available: Boolean) {
    Full(true),
    Lite(true)
}
