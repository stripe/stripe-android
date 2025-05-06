package com.stripe.android.paymentsheet.utils

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal val PaymentSheet.GooglePayConfiguration.ButtonType?.asGooglePayButtonType: GooglePayButtonType
    get() = when (this) {
        PaymentSheet.GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
        PaymentSheet.GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
        PaymentSheet.GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
        PaymentSheet.GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
        PaymentSheet.GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
        PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
        PaymentSheet.GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
        PaymentSheet.GooglePayConfiguration.ButtonType.Pay,
        null -> GooglePayButtonType.Pay
    }
