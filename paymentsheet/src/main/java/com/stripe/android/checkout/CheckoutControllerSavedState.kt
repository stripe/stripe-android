package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle

@Suppress("RestrictedApi")
internal class CheckoutControllerSavedState(
    private val parentHandle: SavedStateHandle,
    private val integrationName: String,
) {
    val handle: SavedStateHandle = SavedStateHandle.createHandle(
        restoredState = parentHandle.remove<Bundle>(integrationName),
        defaultState = null,
    ).also { childHandle ->
        parentHandle.setSavedStateProvider(integrationName) {
            childHandle.savedStateProvider().saveState()
        }
    }

    fun clear() {
        parentHandle.clearSavedStateProvider(integrationName)
        parentHandle.remove<Bundle>(integrationName)
    }
}
