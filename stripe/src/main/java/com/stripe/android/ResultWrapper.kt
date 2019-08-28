package com.stripe.android

internal data class ResultWrapper<ResultType> constructor(
    val result: ResultType? = null,
    val error: Exception? = null
) {
    companion object {
        @JvmStatic
        fun <ResultType> create(result: ResultType?): ResultWrapper<ResultType> {
            return ResultWrapper(result = result)
        }

        @JvmStatic
        fun <ResultType> create(error: Exception): ResultWrapper<ResultType> {
            return ResultWrapper(error = error)
        }
    }
}
