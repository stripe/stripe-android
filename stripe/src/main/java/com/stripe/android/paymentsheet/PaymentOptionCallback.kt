package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentOption

fun interface PaymentOptionCallback {
    fun onPaymentOption(paymentOption: PaymentOption?)
}
