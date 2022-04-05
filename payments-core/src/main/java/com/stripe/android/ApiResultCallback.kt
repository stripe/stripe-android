package com.stripe.android

import com.stripe.android.core.model.StripeModel

/**
 * Generic interface for an API operation callback that either returns a
 * result, [ResultType], or an [Exception]
 */
interface ApiResultCallback<in ResultType : StripeModel> {
    fun onSuccess(result: ResultType)

    fun onError(e: Exception)
}
