package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution

internal class NoSupportedPaymentMethodTypeAccountsException(
    val accountsCount: Int,
    val institution: FinancialConnectionsInstitution,
    val merchantName: String,
    stripeException: StripeException
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
)
