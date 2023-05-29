package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException

/**
 * Base class for errors that occur during the financial connections flow.
 */
internal abstract class FinancialConnectionsStripeError(
    val name: String,
    stripeException: StripeException,
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
)
