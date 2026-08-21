package com.stripe.android.checkout

import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal fun CheckoutGooglePayConfiguration.ButtonType.asGooglePayButtonType(): GooglePayButtonType {
    return when (this) {
        CheckoutGooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
        CheckoutGooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
        CheckoutGooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
        CheckoutGooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
        CheckoutGooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
        CheckoutGooglePayConfiguration.ButtonType.Pay -> GooglePayButtonType.Pay
        CheckoutGooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
        CheckoutGooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
    }
}
