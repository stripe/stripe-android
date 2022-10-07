package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class NoAccountsAvailableException(
    val allowManualEntry: Boolean,
    val canRetry: Boolean,
    val institution: FinancialConnectionsInstitution,
    stripeException: StripeException
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
)
