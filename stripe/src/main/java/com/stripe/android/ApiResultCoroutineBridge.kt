package com.stripe.android

import com.stripe.android.model.StripeModel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Generic class to bridge an API operation callback with coroutines.
 *
 * @param continuation [Continuation] obtained from [suspendCoroutine]
 */
class ApiResultCoroutineBridge<in ResultType : StripeModel>(
    private val continuation: Continuation<ResultType>
) : ApiResultCallback<ResultType> {

    override fun onSuccess(result: ResultType) {
        continuation.resume(result)
    }

    override fun onError(e: Exception) {
        continuation.resumeWithException(e)
    }
}

/**
 * Suspend the currently running coroutine until one of the [ApiResultCallback] methods are invoked.
 */
suspend inline fun <ResultType : StripeModel> suspendApiResultCoroutine(
    crossinline block: (callback: ApiResultCallback<ResultType>) -> Unit
): ResultType {
    return suspendCoroutine { cont ->
        block(ApiResultCoroutineBridge(cont))
    }
}
