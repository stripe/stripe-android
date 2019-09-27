package com.stripe.android.exception

/**
 * An [Exception] that represents a failure to connect to Stripe's API.
 */
class APIConnectionException(
    message: String?,
    e: Throwable?
) : StripeException(null, message, null, STATUS_CODE, e) {
    companion object {
        private const val STATUS_CODE = 0

        @JvmStatic
        fun create(url: String, e: Exception): APIConnectionException {
            return APIConnectionException(
                "IOException during API request to Stripe ($url): ${e.message}. " +
                    "Please check your internet connection and try again. " +
                    "If this problem persists, you should check Stripe's " +
                    "service status at https://twitter.com/stripestatus, " +
                    "or let us know at support@stripe.com.",
                e
            )
        }
    }
}
