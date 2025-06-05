package com.stripe.android.link.verification

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation for surfaces that don't support Link embedded verification.
 */
internal class NoOpLinkEmbeddedInteractor : LinkEmbeddedInteractor {

    override val state: StateFlow<LinkEmbeddedState> = MutableStateFlow(
        LinkEmbeddedState(verificationState = VerificationState.Resolved)
    )

    override fun setup(paymentMethodMetadata: PaymentMethodMetadata) {
        // No-op implementation
    }
}