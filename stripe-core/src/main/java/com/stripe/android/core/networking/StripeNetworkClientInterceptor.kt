package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.stripe.android.core.exception.APIConnectionException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeNetworkClientInterceptor {

    private var delay: Long = 1_000L
    private var shouldFail: (String) -> Boolean = { false }

    fun setDelay(delayInMillis: Long) {
        this.delay = delayInMillis
    }

    fun setFailureEvaluator(evaluator: (requestUrl: String) -> Boolean) {
        this.shouldFail = evaluator
    }

    @WorkerThread
    internal fun throwErrorOrDoNothing(requestUrl: String) {
        val shouldThrow = shouldFail(requestUrl)
        if (shouldThrow) {
            // This is being called on an IO thread, meaning that we can add the delay without
            // danger of NetworkOnMainThreadException.
            Thread.sleep(delay)
            throw APIConnectionException()
        }
    }
}
