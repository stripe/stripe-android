package com.stripe.android

import com.stripe.android.exception.StripeException

internal data class ResultWrapper<ResultType> internal constructor(
    val result: ResultType? = null,
    val error: StripeException? = null
) {
    internal companion object {
        @JvmSynthetic
        internal fun <ResultType> create(result: ResultType?): ResultWrapper<ResultType> {
            return ResultWrapper(result = result)
        }

        @JvmSynthetic
        internal fun <ResultType> create(error: StripeException): ResultWrapper<ResultType> {
            return ResultWrapper(error = error)
        }
    }
}
