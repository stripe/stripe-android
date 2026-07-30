package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.GooglePayButtonType

@OptIn(CheckoutSessionPreview::class)
internal fun GooglePayConfiguration.ButtonType.asGooglePayButtonType(): GooglePayButtonType {
    return when (this) {
        GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
        GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
        GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
        GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
        GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
        GooglePayConfiguration.ButtonType.Pay -> GooglePayButtonType.Pay
        GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
        GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
    }
}
