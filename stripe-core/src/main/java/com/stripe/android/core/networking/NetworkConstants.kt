package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

/**
 * If the SDK receives a "Too Many Requests" (429) status code from Stripe,
 * it will automatically retry the request using exponential backoff.
 *
 * See https://stripe.com/docs/rate-limits for more information.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HTTP_TOO_MANY_REQUESTS = 429
