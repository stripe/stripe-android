package com.stripe.android.paymentsheet.analytics

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {
    fun onInit(configuration: PaymentSheet.Configuration?)

    fun onDismiss()

    fun onShowExistingPaymentOptions()

    fun onShowNewPaymentOptionForm()

    fun onSelectPaymentOption(paymentSelection: PaymentSelection)

    fun onPaymentSuccess(paymentSelection: PaymentSelection?)

    fun onPaymentFailure(paymentSelection: PaymentSelection?)

    enum class Mode(val code: String) {
        Complete("complete"),
        Custom("custom");

        override fun toString(): String = code
    }
}
