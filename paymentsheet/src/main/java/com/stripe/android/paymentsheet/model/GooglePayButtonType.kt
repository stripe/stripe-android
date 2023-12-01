package com.stripe.android.paymentsheet.model

import com.google.android.gms.wallet.button.ButtonConstants

internal enum class GooglePayButtonType(val value: Int) {
    Buy(ButtonConstants.ButtonType.BUY),
    Book(ButtonConstants.ButtonType.BOOK),
    Checkout(ButtonConstants.ButtonType.CHECKOUT),
    Donate(ButtonConstants.ButtonType.DONATE),
    Order(ButtonConstants.ButtonType.ORDER),
    Pay(ButtonConstants.ButtonType.PAY),
    Subscribe(ButtonConstants.ButtonType.SUBSCRIBE),
    Plain(ButtonConstants.ButtonType.PLAIN)
}
