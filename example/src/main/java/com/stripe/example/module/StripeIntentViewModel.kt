package com.stripe.example.module

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.createPaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.R
import com.stripe.example.StripeFactory
import com.stripe.example.activity.BaseViewModel
import kotlinx.coroutines.launch
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
    val requiresAction = MutableLiveData<Boolean>()
    var piSecret: String? = null
    val stripe = StripeFactory(application).create()

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

    private fun confirmPaymentIntentWithPaymentMethod(paymentMethodId: String) {
        println("StripeSdk confirming payment method id: $paymentMethodId")
        status.postValue("Creating a confirming intent server side")
        viewModelScope.launch {
            runCatching {
                backendApi.confirmPaymentIntent(
                    mapOf("payment_method_id" to paymentMethodId).toMutableMap()
                )
            }.onSuccess {
                status.postValue("Confirmation succeeded")
                val response = JSONObject(it.string())
                if (response.optBoolean("requires_action")) {
                    piSecret = response.getString("secret")
                    requiresAction.postValue(true)
                }
            }.onFailure {
                status.postValue("Confirmation failed: ${it.message}")
                println(it)
            }
        }
    }

    fun confirmPaymentIntentWithIntentId() {
        val intentId = getIntentId(piSecret!!)
        println("StripeSdk confirming intent id: $intentId")
        status.postValue("Confirming intent after 3ds2 auth: $intentId")
        viewModelScope.launch {
            runCatching {
                backendApi.confirmPaymentIntent(
                    mapOf("payment_intent_id" to intentId).toMutableMap()
                )
            }.onSuccess {
                val response = JSONObject(it.string())
                status.postValue("Confirming intent after 3ds2 auth success: $response")
                requiresAction.postValue(false)
                piSecret = null
            }.onFailure {
                status.postValue("Confirming intent after 3ds2 auth failure: ${it.message}")
                println(it)
            }
        }
    }

    private fun getIntentId(s: String): String {
        var underscoreCount = 0
        var i = 0
        while (underscoreCount < 2) {
            if (s[i] == '_') underscoreCount++
            i++
        }
        return s.slice(0..i - 2)
    }

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ) {
        viewModelScope.launch {
            runCatching {
                stripe.createPaymentMethod(params)
            }.onSuccess { paymentMethod ->
                confirmPaymentIntentWithPaymentMethod(paymentMethod.id!!)
            }.onFailure {
                println(it)
            }
        }
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
