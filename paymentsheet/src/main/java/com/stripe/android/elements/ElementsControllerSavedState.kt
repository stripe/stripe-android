package com.stripe.android.elements

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle

/** Isolates one controller's saved state from the rest of its owner's state. */
@Suppress("RestrictedApi")
internal class ElementsControllerSavedState(
    private val parentHandle: SavedStateHandle,
    private val integrationName: String,
) {
    val handle = SavedStateHandle.createHandle(
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
