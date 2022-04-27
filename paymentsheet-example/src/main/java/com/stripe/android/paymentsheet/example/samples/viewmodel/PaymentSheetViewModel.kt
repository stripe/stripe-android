package com.stripe.android.paymentsheet.example.samples.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

internal class PaymentSheetViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()
    val exampleCheckoutResponse = MutableLiveData<ExampleCheckoutResponse>()
    val appearance = MutableLiveData(PaymentSheet.Appearance())

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
    data class ExampleCheckoutResponse(
        val publishableKey: String,
        val paymentIntent: String,
        val customer: String? = null,
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
