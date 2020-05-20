package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject

internal class FragmentExamplesViewModel(
    application: Application
) : BaseViewModel(application) {
    val paymentIntentResultLiveData = MutableLiveData<Result<PaymentIntentResult>>()
    val setupIntentResultLiveData = MutableLiveData<Result<SetupIntentResult>>()

    fun createPaymentIntent(): LiveData<Result<JSONObject>> {
        return createIntent { backendApi.createPaymentIntent(PARAMS) }
    }

    fun createSetupIntent(): LiveData<Result<JSONObject>> {
        return createIntent { backendApi.createSetupIntent(PARAMS) }
    }

    private fun createIntent(
        apiMethod: suspend () -> ResponseBody
    ): LiveData<Result<JSONObject>> {
        val result = MutableLiveData<Result<JSONObject>>()
        workScope.launch {
            result.postValue(runCatching {
                JSONObject(apiMethod().string())
            })
        }
        return result
    }

    private companion object {
        private val PARAMS = mutableMapOf("country" to "us")
    }
}
