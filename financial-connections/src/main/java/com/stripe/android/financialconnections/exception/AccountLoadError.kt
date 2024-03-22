package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class AccountLoadError(
    val showManualEntry: Boolean, // TODO Remove in favor of allowManualEntry
    val canRetry: Boolean,
    val institution: FinancialConnectionsInstitution,
    stripeException: StripeException
) : FinancialConnectionsError(
    name = "AccountLoadError",
    stripeException = stripeException,
    allowManualEntry = showManualEntry,
)
