package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentOption

/**
 * Callback that is invoked when the customer's [PaymentOption] selection changes.
 */
fun interface PaymentOptionCallback {

    /**
     * @param paymentOption The new [PaymentOption] selection. If null, the customer has not yet
     * selected a [PaymentOption]. The customer can only complete the transaction if this value is
     * not null.
     */
    fun onPaymentOption(paymentOption: PaymentOption?)
}
