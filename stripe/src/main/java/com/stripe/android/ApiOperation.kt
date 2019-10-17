package com.stripe.android

import android.os.AsyncTask
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.StripeException
import java.io.IOException
import org.json.JSONException

internal abstract class ApiOperation<ResultType>(
    private val callback: ApiResultCallback<ResultType>
) : AsyncTask<Void, Void, ResultWrapper<ResultType>>() {

    internal abstract fun getResult(): ResultType?

    override fun doInBackground(vararg voids: Void): ResultWrapper<ResultType> {
        return try {
            ResultWrapper.create(getResult())
        } catch (e: StripeException) {
            ResultWrapper.create(e)
        } catch (e: JSONException) {
            ResultWrapper.create(e)
        } catch (e: IOException) {
            ResultWrapper.create(APIConnectionException.create(e))
        }
    }

    override fun onPostExecute(resultWrapper: ResultWrapper<ResultType>) {
        super.onPostExecute(resultWrapper)
        when {
            resultWrapper.result != null -> callback.onSuccess(resultWrapper.result)
            resultWrapper.error != null -> callback.onError(resultWrapper.error)
            else -> callback.onError(RuntimeException(
                "The API operation returned neither a result or exception"))
        }
    }
}
