package com.stripe.android.exception

/**
 * An [Exception] that represents max retry is reached when making a request.
 */
internal class MaxRetryReachedException(message: String? = null) :
    StripeException(message = message)
