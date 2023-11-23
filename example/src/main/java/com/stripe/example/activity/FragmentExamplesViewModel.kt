package com.stripe.example.activity

import android.app.Application
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject

internal class FragmentExamplesViewModel(
    application: Application
) : BaseViewModel(application) {
    val paymentIntentResult = MutableStateFlow<FragmentExamplesIntentResult<PaymentIntentResult>>(
        FragmentExamplesIntentResult.None()
    )
    val setupIntentResult = MutableStateFlow<FragmentExamplesIntentResult<SetupIntentResult>>(
        FragmentExamplesIntentResult.None()
    )

    suspend fun createPaymentIntent() = createIntent { backendApi.createPaymentIntent(PARAMS) }

    suspend fun createSetupIntent() = createIntent { backendApi.createSetupIntent(PARAMS) }

    private suspend fun createIntent(
        apiMethod: suspend () -> ResponseBody
    ): Result<JSONObject> {
        return withContext(workContext) {
            runCatching {
                JSONObject(apiMethod().string())
            }
        }
    }

    private companion object {
        private val PARAMS = mutableMapOf("country" to "us")
    }

    internal sealed interface FragmentExamplesIntentResult<Intent> {
        class None<Intent> : FragmentExamplesIntentResult<Intent>

        data class Available<Intent>(val result: Result<Intent>) : FragmentExamplesIntentResult<Intent>
    }
}
