package com.stripe.android.checkout

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal fun PaymentSheet.GooglePayConfiguration.ButtonType.asGooglePayButtonType(): GooglePayButtonType {
    return when (this) {
        PaymentSheet.GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
        PaymentSheet.GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
        PaymentSheet.GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
        PaymentSheet.GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
        PaymentSheet.GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
        PaymentSheet.GooglePayConfiguration.ButtonType.Pay -> GooglePayButtonType.Pay
        PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
        PaymentSheet.GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
    }
}
