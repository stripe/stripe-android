package com.stripe.android.networking

internal const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Payment will only retry if server responses with [HTTP_TOO_MANY_REQUESTS].
 */
internal val PAYMENT_RETRY_CODES: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS
