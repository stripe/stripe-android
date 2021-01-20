package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentOption

internal fun interface PaymentOptionCallback {
    fun onPaymentOption(paymentOption: PaymentOption?)
}
