package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.createPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.StripeFactory
import kotlinx.coroutines.launch

internal class PaymentMethodViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): LiveData<Result<PaymentMethod>> {
        val resultData = MutableLiveData<Result<PaymentMethod>>()
        viewModelScope.launch {
            resultData.value = runCatching {
                stripe.createPaymentMethod(params)
            }
        }
        return resultData
    }
}
