package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.StripeFactory

internal class PaymentMethodViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): LiveData<Result<PaymentMethod>> {
        val resultData = MutableLiveData<Result<PaymentMethod>>()
        stripe.createPaymentMethod(
            paymentMethodCreateParams = params,
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    resultData.value = Result.success(result)
                }

                override fun onError(e: Exception) {
                    resultData.value = Result.failure(e)
                }
            })
        return resultData
    }
}
