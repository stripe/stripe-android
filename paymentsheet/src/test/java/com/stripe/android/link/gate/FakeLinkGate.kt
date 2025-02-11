package com.stripe.android.link.gate

internal class FakeLinkGate : LinkGate {
    private var _useNativeLink = true
    override val useNativeLink: Boolean
        get() = _useNativeLink

    private var _useAttestationEndpoints = true
    override val useAttestationEndpoints: Boolean
        get() = _useAttestationEndpoints

    private var _suppress2faModal: Boolean = false
    override val suppress2faModal: Boolean
        get() = _suppress2faModal

    fun setUseNativeLink(value: Boolean) {
        _useNativeLink = value
    }

    fun setUseAttestationEndpoints(value: Boolean) {
        _useAttestationEndpoints = value
    }

    fun setSuppress2faModal(value: Boolean) {
        _suppress2faModal = value
    }
}
