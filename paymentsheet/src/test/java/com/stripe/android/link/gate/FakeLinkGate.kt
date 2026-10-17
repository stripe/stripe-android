package com.stripe.android.link.gate

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.ElementsSession

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

    private var _useInlineOtpInWalletButtons: Boolean = false
    override val useInlineOtpInWalletButtons: Boolean
        get() = _useInlineOtpInWalletButtons

    private var _showRuxInFlowController: Boolean = true
    override val showRuxInFlowController: Boolean
        get() = _showRuxInFlowController

    fun setUseNativeLink(value: Boolean) {
        _useNativeLink = value
    }

    fun setUseAttestationEndpoints(value: Boolean) {
        _useAttestationEndpoints = value
    }

    fun setSuppress2faModal(value: Boolean) {
        _suppress2faModal = value
    }

    fun setUseInlineOtpInWalletButtons(value: Boolean) {
        _useInlineOtpInWalletButtons = value
    }

    fun setShowRuxInFlowController(value: Boolean) {
        _showRuxInFlowController = value
    }

    class Factory(val linkGate: LinkGate = FakeLinkGate()) : LinkGate.Factory {
        override fun create(elementsSession: ElementsSession): LinkGate = linkGate
        override fun create(configuration: LinkConfiguration): LinkGate = linkGate
    }
}
