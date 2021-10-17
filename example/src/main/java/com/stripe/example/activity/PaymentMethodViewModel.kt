package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.stripe.android.createPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.StripeFactory

internal class PaymentMethodViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ) = liveData<Result<PaymentMethod>> {
        emit(
            runCatching {
                stripe.createPaymentMethod(params)
            }
        )
    }
}
