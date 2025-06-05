package com.stripe.android.link.verification

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.flow.StateFlow

internal interface LinkEmbeddedInteractor {

    val state: StateFlow<LinkEmbeddedState>

    /**
     * Sets up Link domain logic (should be called once when initializing).
     */
    fun setup(paymentMethodMetadata: PaymentMethodMetadata)
}
