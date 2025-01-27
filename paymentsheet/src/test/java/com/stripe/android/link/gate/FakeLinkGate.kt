package com.stripe.android.link.gate

internal class FakeLinkGate : LinkGate {
    private var _useNativeLink = true
    override val useNativeLink: Boolean
        get() = _useNativeLink

    private var _useAttestationEndpoints = true
    override val useAttestationEndpoints: Boolean
        get() = _useAttestationEndpoints

    fun setUseNativeLink(value: Boolean) {
        _useNativeLink = value
    }

    fun setUseAttestationEndpoints(value: Boolean) {
        _useAttestationEndpoints = value
    }
}
