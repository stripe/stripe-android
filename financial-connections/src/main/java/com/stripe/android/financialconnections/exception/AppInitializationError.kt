package com.stripe.android.financialconnections.exception

import com.stripe.android.core.exception.StripeException

class AppInitializationError(message: String) : StripeException(
    message = message,
    cause = null,
    requestId = null,
    statusCode = 0,
    stripeError = null
)
