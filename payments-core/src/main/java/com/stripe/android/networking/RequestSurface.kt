package com.stripe.android.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class RequestSurface(val value: String) {
    PaymentElement("android_payment_element"),
    CryptoOnramp("android_crypto_onramp"),
    ;

    override fun toString(): String = value
}
