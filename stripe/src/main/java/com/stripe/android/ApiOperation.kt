package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

internal abstract class ApiOperation<ResultType>(
    private val workScope: CoroutineScope = CoroutineScope(IO),
    private val callback: ApiResultCallback<ResultType>
) {
    internal abstract suspend fun getResult(): ResultType?

    internal fun execute() {
        workScope.launch {
            val resultWrapper: ResultWrapper<ResultType> = try {
                ResultWrapper.create(getResult())
            } catch (e: StripeException) {
                ResultWrapper.create(e)
            } catch (e: JSONException) {
                ResultWrapper.create(APIException(e))
            } catch (e: IOException) {
                ResultWrapper.create(APIConnectionException.create(e))
            } catch (e: IllegalArgumentException) {
                ResultWrapper.create(
                    InvalidRequestException(
                        message = e.message,
                        cause = e
                    )
                )
            }

            withContext(Main) {
                dispatchResult(resultWrapper)
            }
        }
    }

    private fun dispatchResult(resultWrapper: ResultWrapper<ResultType>) {
        when {
            resultWrapper.result != null -> callback.onSuccess(resultWrapper.result)
            resultWrapper.error != null -> callback.onError(resultWrapper.error)
            else -> callback.onError(
                RuntimeException("The API operation returned neither a result or exception")
            )
        }
    }
}
