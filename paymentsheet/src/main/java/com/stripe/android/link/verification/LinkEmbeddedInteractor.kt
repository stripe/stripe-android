package com.stripe.android.link.verification

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.StateFlow

internal interface LinkEmbeddedInteractor {

    val state: StateFlow<LinkEmbeddedState>

    val otpElement: OTPElement

    /**
     * Sets up Link domain logic (should be called once when initializing).
     */
    fun setup(paymentMethodMetadata: PaymentMethodMetadata)
}
