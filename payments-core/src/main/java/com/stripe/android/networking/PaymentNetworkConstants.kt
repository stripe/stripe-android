package com.stripe.android.networking

import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS

/**
 * Payment will only retry if server responses with [HTTP_TOO_MANY_REQUESTS].
 */
internal val PAYMENT_RETRY_CODES: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS
