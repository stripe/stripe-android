package com.stripe.android.exception

import com.stripe.android.StripeError
import java.util.Objects

/**
 * A base class for Stripe-related exceptions.
 */
abstract class StripeException(
    val stripeError: StripeError? = null,
    val requestId: String? = null,
    val statusCode: Int = 0,
    cause: Throwable? = null,
    message: String? = stripeError?.message
) : Exception(message, cause) {
    override fun toString(): String {
        return listOfNotNull(
            requestId?.let { "Request-id: $it" },
            super.toString()
        ).joinToString(separator = "\n")
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is StripeException -> typedEquals(other)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(stripeError, requestId, statusCode, message)
    }

    private fun typedEquals(ex: StripeException): Boolean {
        return stripeError == ex.stripeError &&
            requestId == ex.requestId &&
            statusCode == ex.statusCode &&
            message == ex.message
    }
}
