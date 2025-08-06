package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.elements.customersheet.CustomerSheet

internal data class CustomerSheetState(
    val selectedPaymentOption: CustomerSheet.PaymentOptionDisplayData? = null,
    val shouldFetchPaymentOption: Boolean = true
)

internal fun CustomerSheetState?.paymentMethodLabel(): String {
    return this?.selectedPaymentOption?.label ?: "Select"
}

@Composable
internal fun CustomerSheetState?.paymentMethodPainter(): Painter? {
    return this?.selectedPaymentOption?.iconPainter
}
