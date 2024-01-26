package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import org.json.JSONException
import java.io.IOException
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isClientError = statusCode in 400..499

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun analyticsValue(): String = "stripeException"

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun create(throwable: Throwable): StripeException {
            return when (throwable) {
                is StripeException -> throwable
                is JSONException -> APIException(throwable)
                is IOException -> APIConnectionException.create(throwable)
                is IllegalArgumentException -> InvalidRequestException(
                    message = throwable.message,
                    cause = throwable
                )
                else -> APIException(throwable)
            }
        }
    }
}
