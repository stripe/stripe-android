package com.stripe.android.financialconnections.exception

internal class PartnerAuthError(message: String?) : FinancialConnectionsError(
    name = "PartnerAuthError",
    message = message,
)
