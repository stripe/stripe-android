package com.stripe.android.paymentsheet.utils

import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal val GooglePayConfiguration.ButtonType?.asGooglePayButtonType: GooglePayButtonType
    get() = when (this) {
        GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
        GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
        GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
        GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
        GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
        GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
        GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
        GooglePayConfiguration.ButtonType.Pay,
        null -> GooglePayButtonType.Pay
    }
