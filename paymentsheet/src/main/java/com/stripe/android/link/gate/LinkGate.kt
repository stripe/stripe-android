package com.stripe.android.link.gate

internal interface LinkGate {
    val useNativeLink: Boolean
    val useAttestationEndpoints: Boolean
}
