package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class AccountNumberRetrievalError(
    val showManualEntry: Boolean, // TODO
    val institution: FinancialConnectionsInstitution,
    stripeException: StripeException
) : FinancialConnectionsError(
    name = "AccountNumberRetrievalError",
    stripeException = stripeException,
    allowManualEntry = showManualEntry,
)
