package com.stripe.android.exception

import com.stripe.android.core.exception.StripeException

/**
 * An [Exception] that represents max retry is reached when making a request.
 */
internal class MaxRetryReachedException(message: String? = null) :
    StripeException(message = message)
