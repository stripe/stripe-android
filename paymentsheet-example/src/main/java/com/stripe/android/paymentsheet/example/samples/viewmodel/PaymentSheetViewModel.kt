package com.stripe.android.paymentsheet.example.samples.viewmodel

import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

internal class PaymentSheetViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()
    val exampleCheckoutResponse = MutableLiveData<ExampleCheckoutResponse>()

    fun statusDisplayed() {
        status.value = ""
    }

    fun prepareCheckout(backendUrl: String) {
        inProgress.postValue(true)

        Fuel.post(backendUrl)
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        status.postValue("${status.value}\n\nPreparing checkout failed\n" +
                            "${result.getException().message}")
                    }
                    is Result.Success -> {
                        exampleCheckoutResponse.postValue(
                            Gson().fromJson(result.get(), ExampleCheckoutResponse::class.java)
                        )
                    }
                }
                inProgress.postValue(false)
            }
    }

    @Serializable
    @Keep
    data class ExampleCheckoutResponse(
        @SerializedName("publishableKey")
        val publishableKey: String,
        @SerializedName("paymentIntent")
        val paymentIntent: String,
        @SerializedName("customer")
        val customer: String? = null,
        @SerializedName("ephemeralKey")
        val ephemeralKey: String? = null
    ) {
        internal fun makeCustomerConfig() =
            if (customer != null && ephemeralKey != null) {
                PaymentSheet.CustomerConfiguration(
                    id = customer,
                    ephemeralKeySecret = ephemeralKey
                )
            } else {
                null
            }
    }
}
