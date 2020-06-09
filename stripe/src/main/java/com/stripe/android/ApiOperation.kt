package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.StripeModel
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

internal abstract class ApiOperation<out ResultType : StripeModel>(
    private val workScope: CoroutineScope = CoroutineScope(IO),
    private val callback: ApiResultCallback<ResultType>
) {
    internal abstract suspend fun getResult(): ResultType?

    internal fun execute() {
        workScope.launch {
            val result: Result<ResultType?> = try {
                Result.success(getResult())
            } catch (e: StripeException) {
                Result.failure(e)
            } catch (e: JSONException) {
                Result.failure(APIException(e))
            } catch (e: IOException) {
                Result.failure(APIConnectionException.create(e))
            } catch (e: IllegalArgumentException) {
                Result.failure(
                    InvalidRequestException(
                        message = e.message,
                        cause = e
                    )
                )
            }

            // dispatch the API operation result to the main thread
            withContext(Main) {
                dispatchResult(result)
            }
        }
    }

    private fun dispatchResult(result: Result<ResultType?>) {
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
