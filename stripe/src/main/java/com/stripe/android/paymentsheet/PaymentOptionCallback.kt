package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentOption

/**
 * Callback that is invoked when the customer's [PaymentOption] selection changes.
 */
fun interface PaymentOptionCallback {
    fun onPaymentOption(paymentOption: PaymentOption?)
}
