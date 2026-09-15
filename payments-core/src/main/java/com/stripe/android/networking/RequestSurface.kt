package com.stripe.android.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class RequestSurface(val value: String) {
    PaymentElement("android_payment_element"),
    CryptoOnramp("android_crypto_onramp"),

    // Only for use by the LinkControllerPreview API.
    StandaloneLink("android_link_standalone"),

    // Stripe Identity (Networked Identity).
    // #TODO - Networked Identity [NI-Contract]: restore "android_identity_product" once the backend
    // accepts it. It is agreed but not deployed yet, and the API rejects it today. The crypto onramp
    // surface is borrowed meanwhile because it is allowed to sign up on the non-attested endpoints.
    Identity("android_crypto_onramp"),
    ;

    override fun toString(): String = value
}
