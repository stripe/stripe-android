package com.stripe.android.financialconnections.exception

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException

/**
 * Base class for errors that occur during the financial connections flow.
 */
internal abstract class FinancialConnectionsError(
    val name: String,
    val stripeException: StripeException,
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
) {

    override fun analyticsValue(): String = "fcError"

    constructor(
        name: String,
        stripeError: StripeError? = null,
        requestId: String? = null,
        statusCode: Int = 0,
        cause: Throwable? = null,
        message: String? = stripeError?.message
    ) : this(
        name = name,
        stripeException = object : StripeException(
            stripeError = stripeError,
            requestId = requestId,
            statusCode = statusCode,
            cause = cause,
            message = message
        ) {}
    )
}
