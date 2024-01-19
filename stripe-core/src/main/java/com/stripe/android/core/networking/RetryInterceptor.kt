package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.net.ConnectException
import java.net.SocketTimeoutException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RetryInterceptor {

    /**
     * @param request the [StripeRequest] that was made
     * @param result the [Result] of the request.
     *  - If the request was successful, this will be the [StripeResponse] that was returned
     *  - If the request failed, this will be the [Exception] that was thrown
     * @return `true` if the request can be retried, `false` otherwise
     */
    fun <BodyType> shouldRetry(
        request: StripeRequest,
        result: Result<StripeResponse<BodyType>>
    ): Boolean = result.fold(
        onSuccess = { request.retryResponseCodes.contains(it.code) },
        onFailure = { it is SocketTimeoutException || it is ConnectException }
    )
}
