package com.stripe.android.financialconnections.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

/**
 * Base class for errors that occur during the financial connections flow.
 */
internal abstract class FinancialConnectionsError(
    val name: String,
    stripeException: StripeException,
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String? = null
}
