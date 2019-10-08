package com.stripe.android

/**
 * Generic interface for an API operation callback that either returns a
 * result, [ResultType], or an [Exception]
 */
interface ApiResultCallback<ResultType> {
    fun onSuccess(result: ResultType)

    fun onError(e: Exception)
}
