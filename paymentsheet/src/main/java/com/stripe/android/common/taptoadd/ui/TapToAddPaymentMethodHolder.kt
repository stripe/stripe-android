package com.stripe.android.common.taptoadd.ui

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject
import javax.inject.Singleton

internal interface TapToAddPaymentMethodHolder {
    val paymentMethod: PaymentMethod?

    fun setPaymentMethod(paymentMethod: PaymentMethod?)
}

@Singleton
internal class DefaultTapToAddPaymentMethodHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : TapToAddPaymentMethodHolder {
    override val paymentMethod: PaymentMethod?
        get() = savedStateHandle[TAP_TO_ADD_PAYMENT_METHOD_KEY]

    override fun setPaymentMethod(paymentMethod: PaymentMethod?) {
        savedStateHandle[TAP_TO_ADD_PAYMENT_METHOD_KEY] = paymentMethod
    }

    private companion object {
        const val TAP_TO_ADD_PAYMENT_METHOD_KEY = "STRIPE_TAP_TO_ADD_PAYMENT_METHOD"
    }
}
