package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.core.exception.RateLimitException
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeNetworkClientInterceptor {

    private var delay: Long = 1_000L
    private var shouldFail: (String) -> Boolean = { false }

    var errorType: ErrorType = ErrorType.Network
        private set

    fun setErrorToThrow(errorType: ErrorType) {
        this.errorType = errorType
    }

    fun setDelay(delayInMillis: Long) {
        this.delay = delayInMillis
    }

    fun setFailureEvaluator(evaluator: (requestUrl: String) -> Boolean) {
        this.shouldFail = evaluator
    }

    internal fun throwErrorOrDoNothing(requestUrl: String) {
        val shouldThrow = shouldFail(requestUrl)
        if (shouldThrow) {
            // TODO: Comment about off main thread
            Thread.sleep(delay)
            throw errorType.asException
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ErrorType {
        Authentication,
        InvalidRequest,
        InvalidResponse,
        Permission,
        Network,
        TooManyRequests,
        Unknown;

        val asException: Throwable
            // TODO: Make sure there are none missing
            get() = when (this) {
                Authentication -> AuthenticationException(stripeError = StripeError())
                InvalidRequest -> InvalidRequestException()
                InvalidResponse -> APIException()
                Permission -> PermissionException(stripeError = StripeError())
                Network -> IOException("Interceptor error")
                TooManyRequests -> RateLimitException()
                Unknown -> RuntimeException("Interceptor error")
            }
    }
}
