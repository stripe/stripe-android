package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import okio.IOException

/**
 * A base class for Stripe-related exceptions.
 */
abstract class StripeException(
    val stripeError: StripeError? = null,
    val requestId: String? = null,
    val statusCode: Int = DEFAULT_STATUS_CODE,
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
        var result = stripeError.hashCode()
        result = 31 * result + requestId.hashCode()
        result = 31 * result + statusCode
        result = 31 * result + message.hashCode()
        return result
    }

    private fun typedEquals(ex: StripeException): Boolean {
        return stripeError == ex.stripeError &&
            requestId == ex.requestId &&
            statusCode == ex.statusCode &&
            message == ex.message
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val DEFAULT_STATUS_CODE = 0

        fun create(throwable: Throwable): StripeException {
            return when {
                throwable is StripeException -> throwable
                isJsonException(throwable) -> APIException(throwable)
                throwable is IOException -> APIConnectionException.create(throwable)
                throwable is IllegalArgumentException -> InvalidRequestException(
                    message = throwable.message,
                    cause = throwable
                )
                else -> GenericStripeException(throwable, analyticsValue = analyticsValueForThrowable(throwable))
            }
        }

        private fun analyticsValueForThrowable(throwable: Throwable): String? {
            val throwableClassName = throwableClassName(throwable)
            if (
                throwableClassName != null &&
                (
                    throwableClassName.startsWith("android.") ||
                        throwableClassName.startsWith("java.")
                    )
            ) {
                return throwableClassName
            }
            return null
        }
    }
}
