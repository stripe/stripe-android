package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject

internal class FragmentExamplesViewModel(
    application: Application
) : BaseViewModel(application) {
    val paymentIntentResultLiveData = MutableLiveData<Result<PaymentIntentResult>>()
    val setupIntentResultLiveData = MutableLiveData<Result<SetupIntentResult>>()

    fun createPaymentIntent() = createIntent { backendApi.createPaymentIntent(PARAMS) }

    fun createSetupIntent() = createIntent { backendApi.createSetupIntent(PARAMS) }

    private fun createIntent(
        apiMethod: suspend () -> ResponseBody
    ) = liveData {
        withContext(workContext) {
            emit(
                runCatching {
                    JSONObject(apiMethod().string())
                }
            )
        }
    }

    private companion object {
        private val PARAMS = mutableMapOf("country" to "us")
    }
}
