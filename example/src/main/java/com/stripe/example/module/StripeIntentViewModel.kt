package com.stripe.example.module

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.R
import com.stripe.example.activity.BaseViewModel
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException

internal class StripeIntentViewModel(
    application: Application
) : BaseViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val paymentResultLiveData = MutableLiveData<PaymentResult>()

    fun createPaymentIntent(
        country: String,
        customerId: String? = null,
        supportedPaymentMethods: String? = null,
        currency: String? = null,
    ) = makeBackendRequest(
        R.string.creating_payment_intent,
        R.string.payment_intent_status
    ) {
        backendApi.createPaymentIntent(
            mapOf("country" to country)
                .plus(
                    customerId?.let {
                        mapOf("customer_id" to it)
                    }.orEmpty()
                ).plus(
                    supportedPaymentMethods?.let {
                        mapOf("supported_payment_methods" to it)
                    }.orEmpty()
                ).plus(
                    currency?.let {
                        mapOf("currency" to it)
                    }.orEmpty()
                )
                .toMutableMap()
        )
    }

    fun createSetupIntent(
        country: String,
        customerId: String? = null,
        supportedPaymentMethods: String? = null,
    ) = makeBackendRequest(
        R.string.creating_setup_intent,
        R.string.setup_intent_status
    ) {
        backendApi.createSetupIntent(
            mutableMapOf("country" to country)
                .plus(
                    customerId?.let {
                        mapOf("customer_id" to it)
                    }.orEmpty()
                ).plus(
                    supportedPaymentMethods?.let {
                        mapOf("supported_payment_methods" to it)
                    }.orEmpty()
                )
                .toMutableMap()
        )
    }

    private fun makeBackendRequest(
        @StringRes creatingStringRes: Int,
        @StringRes resultStringRes: Int,
        apiMethod: suspend () -> ResponseBody
    ) = liveData {
        inProgress.postValue(true)
        status.postValue(resources.getString(creatingStringRes))

        val result = withContext(workContext) {
            runCatching {
                JSONObject(apiMethod().string())
            }
        }

        result.fold(
            onSuccess = {
                val intentStatus = resources.getString(
                    resultStringRes,
                    it.getString("status")
                )
                status.postValue(
                    """
                    ${status.value}
                    
                    
                    $intentStatus
                    """.trimIndent()
                )
            },
            onFailure = {
                val errorMessage =
                    (it as? HttpException)?.response()?.errorBody()?.string()
                        ?: it.message
                status.postValue(
                    """
                    ${status.value}
                    
                    
                    $errorMessage
                    """.trimIndent()
                )
                inProgress.postValue(false)
            }
        )

        emit(result)
    }
}
