package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.SavedStateHandle

internal class ConfirmationRegistry(
    private val confirmationDefinitions: List<ConfirmationDefinition<*, *, *, *>>,
) {
    fun createConfirmationMediators(
        savedStateHandle: SavedStateHandle
    ): List<ConfirmationMediator<*, *, *, *>> {
        return confirmationDefinitions.map { definition ->
            ConfirmationMediator(savedStateHandle, definition)
        }
    }
}
