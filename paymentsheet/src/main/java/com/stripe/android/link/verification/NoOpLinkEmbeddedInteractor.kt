package com.stripe.android.link.verification

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation for surfaces that don't support Link embedded verification.
 */
internal class NoOpLinkEmbeddedInteractor : LinkEmbeddedInteractor {

    override val state: StateFlow<LinkEmbeddedState> = MutableStateFlow(
        LinkEmbeddedState(verificationState = VerificationState.Resolved)
    )
    override val otpElement: OTPElement = OTPSpec.transform()

    override fun setup(paymentMethodMetadata: PaymentMethodMetadata) {
        // No-op implementation
    }
}
