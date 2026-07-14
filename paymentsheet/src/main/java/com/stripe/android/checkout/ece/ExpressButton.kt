package com.stripe.android.checkout.ece

internal sealed interface ExpressButton {
    data object Link : ExpressButton
    data object GooglePay : ExpressButton
}
