package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import java.util.UUID

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

    val controllerInstanceId: String = handle.get<String>(CONTROLLER_INSTANCE_ID_KEY)
        ?: UUID.randomUUID().toString().also { handle[CONTROLLER_INSTANCE_ID_KEY] = it }

    fun clear() {
        parentHandle.clearSavedStateProvider(integrationName)
        parentHandle.remove<Bundle>(integrationName)
    }

    private companion object {
        const val CONTROLLER_INSTANCE_ID_KEY = "CheckoutControllerSavedState_CONTROLLER_INSTANCE_ID_KEY"
    }
}
