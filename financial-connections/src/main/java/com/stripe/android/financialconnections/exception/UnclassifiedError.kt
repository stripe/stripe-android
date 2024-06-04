package com.stripe.android.financialconnections.exception

internal class UnclassifiedError(
    name: String,
    message: String? = null,
) : FinancialConnectionsError(
    name = name,
    message = message,
)
