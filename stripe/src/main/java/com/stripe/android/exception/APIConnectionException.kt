package com.stripe.android.exception

import java.io.IOException

/**
 * An [Exception] that represents a failure to connect to Stripe's API.
 */
class APIConnectionException(
    message: String? = null,
    cause: Throwable? = null
) : StripeException(
    cause = cause,
    message = message
) {
    internal companion object {
        @JvmSynthetic
        internal fun create(e: IOException, url: String? = null): APIConnectionException {
            val displayUrl = listOfNotNull(
                "Stripe",
                "($url)".takeUnless { url.isNullOrBlank() }
            ).joinToString(" ")
            return APIConnectionException(
                "IOException during API request to $displayUrl: ${e.message}. " +
                    "Please check your internet connection and try again. " +
                    "If this problem persists, you should check Stripe's " +
                    "service status at https://twitter.com/stripestatus, " +
                    "or let us know at support@stripe.com.",
                e
            )
        }
    }
}
