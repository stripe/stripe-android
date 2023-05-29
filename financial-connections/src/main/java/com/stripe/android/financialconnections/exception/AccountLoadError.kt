package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class AccountLoadError(
    val allowManualEntry: Boolean,
    val canRetry: Boolean,
    val institution: FinancialConnectionsInstitution,
    stripeException: StripeException
) : FinancialConnectionsStripeError(
    name = "AccountLoadError",
    stripeException = stripeException
)
