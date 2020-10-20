package com.stripe.android

import com.stripe.android.exception.StripeException
import com.stripe.android.model.StripeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal abstract class ApiOperation<out ResultType : StripeModel>(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val callback: ApiResultCallback<ResultType>
) {
    internal abstract suspend fun getResult(): ResultType?

    internal fun execute() {
        CoroutineScope(workContext).launch {
            val result = kotlin.runCatching {
                getResult()
            }.recoverCatching { throw StripeException.create(it) }

            dispatchResult(result)
        }
    }

    private suspend fun dispatchResult(
        result: Result<ResultType?>
    ) = withContext(Dispatchers.Main) {
        result.fold(
            onSuccess = {
                when {
                    it != null -> callback.onSuccess(it)
                    else -> callback.onError(
                        RuntimeException("The API operation returned neither a result or exception")
                    )
                }
            },
            onFailure = { exception ->
                callback.onError(
                    when (exception) {
                        is StripeException -> exception
                        else -> RuntimeException(exception)
                    }
                )
            }
        )
    }
}
