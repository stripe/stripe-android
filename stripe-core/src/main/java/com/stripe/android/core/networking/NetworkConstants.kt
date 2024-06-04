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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val DEFAULT_RETRY_CODES: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_USER_AGENT = "User-Agent"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_ACCEPT_CHARSET = "Accept-Charset"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_CONTENT_TYPE = "Content-Type"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_ACCEPT = "Accept"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_STRIPE_VERSION = "Stripe-Version"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_STRIPE_ACCOUNT = "Stripe-Account"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_STRIPE_LIVEMODE = "Stripe-Livemode"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_AUTHORIZATION = "Authorization"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_IDEMPOTENCY_KEY = "Idempotency-Key"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val HEADER_X_STRIPE_USER_AGENT = "X-Stripe-User-Agent"
