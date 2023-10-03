package com.stripe.android.paymentsheet.example.playground

import android.graphics.drawable.Drawable
import com.stripe.android.paymentsheet.model.PaymentOption

internal data class FlowControllerState(
    val selectedPaymentOption: PaymentOption? = null,
)

internal fun FlowControllerState?.paymentMethodLabel(): String {
    return if (this == null) {
        "Loading"
    } else {
        selectedPaymentOption?.label ?: "Select"
    }
}

internal fun FlowControllerState?.paymentMethodIcon(): Drawable? {
    return this?.selectedPaymentOption?.icon()
}
