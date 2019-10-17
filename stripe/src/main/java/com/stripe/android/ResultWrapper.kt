package com.stripe.android

internal data class ResultWrapper<ResultType> private constructor(
    val result: ResultType? = null,
    val error: Exception? = null
) {
    companion object {
        @JvmSynthetic
        internal fun <ResultType> create(result: ResultType?): ResultWrapper<ResultType> {
            return ResultWrapper(result = result)
        }

        @JvmSynthetic
        internal fun <ResultType> create(error: Exception): ResultWrapper<ResultType> {
            return ResultWrapper(error = error)
        }
    }
}
