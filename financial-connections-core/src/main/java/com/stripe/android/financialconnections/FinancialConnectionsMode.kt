package com.stripe.android.financialconnections

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class FinancialConnectionsMode(val available: Boolean) {
    Full(true),
    Lite(true),
    None(false)
}
