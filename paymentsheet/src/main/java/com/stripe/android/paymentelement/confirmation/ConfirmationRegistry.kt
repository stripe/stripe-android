package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.SavedStateHandle

internal class ConfirmationRegistry(
    private val confirmationDefinitions: List<ConfirmationDefinition<*, *, *, *>>,
) {
    fun createConfirmationMediators(
        savedStateHandle: SavedStateHandle
    ): List<ConfirmationMediator<*, *, *, *>> {
        /*
         * We need to sort the mediators in order to guarantee the order of registered launchers. See the following
         * documentation regarding this topic: https://developer.android.com/training/basics/intents/result#register
         */
        return confirmationDefinitions.sortedBy { definition ->
            definition.key
        }.map { definition ->
            ConfirmationMediator(savedStateHandle, definition)
        }
    }
}
