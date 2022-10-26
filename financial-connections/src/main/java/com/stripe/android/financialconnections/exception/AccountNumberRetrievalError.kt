package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class AccountNumberRetrievalError(
    val allowManualEntry: Boolean,
    val institution: FinancialConnectionsInstitution,
    stripeException: StripeException
) : FinancialConnectionsError(
    name = "AccountNumberRetrievalError",
    stripeException = stripeException
)
