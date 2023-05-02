package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class AccountNoneEligibleForPaymentMethodError(
    val accountsCount: Int,
    val institution: FinancialConnectionsInstitution,
    val merchantName: String,
    stripeException: StripeException,
) : FinancialConnectionsError(
    name = "AccountNoneEligibleForPaymentMethodError",
    stripeException = stripeException
)

internal abstract class FinancialConnectionsError(
    val name: String,
    stripeException: StripeException,
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
)
