package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentOption

internal data class FlowControllerState(
    val selectedPaymentOption: PaymentOption? = null,
    val addressDetails: AddressDetails? = null,
    val shouldFetchPaymentOption: Boolean = true
)

internal fun FlowControllerState?.paymentMethodLabel(): String {
    return if (this == null) {
        "Loading"
    } else {
        selectedPaymentOption?.label ?: "Select"
    }
}

@Composable
internal fun FlowControllerState?.paymentMethodPainter(): Painter? {
    return this?.selectedPaymentOption?.iconPainter
}
