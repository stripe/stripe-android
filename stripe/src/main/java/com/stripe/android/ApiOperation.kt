package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.StripeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import kotlin.coroutines.CoroutineContext

internal abstract class ApiOperation<out ResultType : StripeModel>(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val callback: ApiResultCallback<ResultType>
) {
    internal abstract suspend fun getResult(): ResultType?

    internal fun execute() {
        CoroutineScope(workContext).launch {
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
