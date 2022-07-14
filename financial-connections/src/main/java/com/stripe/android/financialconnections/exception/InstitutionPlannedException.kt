package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.model.Institution

internal class InstitutionPlannedException(
    val institution: Institution,
    val isToday: Boolean,
    val backUpAt: Long,
    stripeException: StripeException
) : StripeException(
    stripeException.stripeError,
    stripeException.requestId,
    stripeException.statusCode,
    stripeException.cause,
    stripeException.message
)
