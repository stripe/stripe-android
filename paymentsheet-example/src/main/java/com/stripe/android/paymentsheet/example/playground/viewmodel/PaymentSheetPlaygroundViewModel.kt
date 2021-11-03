package com.stripe.android.paymentsheet.example.playground.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse

internal class PaymentSheetPlaygroundViewModel(
    application: Application
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>()
    val clientSecret = MutableLiveData<String?>()

    val readyToCheckout: LiveData<Boolean> = clientSecret.map {
        it != null
    }

    var checkoutMode: CheckoutMode? = null
    var temporaryCustomerId: String? = null

    /**
     * Calls the backend to prepare for checkout. The server creates a new Payment or Setup Intent
     * that will be confirmed on the client using Payment Sheet.
     */
    fun prepareCheckout(
        customer: CheckoutCustomer,
        currency: CheckoutCurrency,
        mode: CheckoutMode,
        setShippingAddress: Boolean,
        setAutomaticPaymentMethod: Boolean,
        backendUrl: String
    ) {
        customerConfig.value = null
        clientSecret.value = null

        inProgress.postValue(true)

        val requestBody = CheckoutRequest(
            customer.value,
            currency.value,
            mode.value,
            setShippingAddress,
            setAutomaticPaymentMethod
        )

        Fuel.post(backendUrl+"checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when(result) {
                    is Result.Failure -> {
                        status.postValue(
                            "Preparing checkout failed:\n${result.getException().message}"
                        )
                    }
                    is Result.Success -> {
                        val checkoutResponse = Gson()
                            .fromJson(result.get(), CheckoutResponse::class.java)
                        checkoutMode = mode
                        temporaryCustomerId = if (customer == CheckoutCustomer.New) {
                            checkoutResponse.customerId
                        } else {
                            null
                        }

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(getApplication(), checkoutResponse.publishableKey)

                        customerConfig.postValue(checkoutResponse.makeCustomerConfig())
                        clientSecret.postValue(checkoutResponse.intentClientSecret)
                    }
                }
                inProgress.postValue(false)
        }
    }
}
