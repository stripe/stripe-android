package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle

internal class PaymentConfirmationRegistry(
    private val confirmationDefinitions: List<PaymentConfirmationDefinition<*, *, *, *>>,
) {
    fun createConfirmationMediators(
        savedStateHandle: SavedStateHandle
    ): List<PaymentConfirmationMediator<*, *, *, *>> {
        return confirmationDefinitions.map { definition ->
            PaymentConfirmationMediator(savedStateHandle, definition)
        }
    }
}
