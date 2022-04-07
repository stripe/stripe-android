package com.stripe.android.connections

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.connections.model.LinkAccountSessionManifest

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class ConnectionsSheetState(
    val activityRecreated: Boolean = false,
    val manifest: LinkAccountSessionManifest? = null,
    val authFlowActive: Boolean = false
) {

    /**
     * Restores existing persisted fields into the current [ConnectionsSheetState]
     */
    internal fun from(savedStateHandle: SavedStateHandle): ConnectionsSheetState {
        return copy(
            manifest = savedStateHandle.get(KEY_MANIFEST)
        )
    }

    /**
     * Saves the persistable fields of this state that changed to the given [SavedStateHandle]
     */
    internal fun to(savedStateHandle: SavedStateHandle, previousValue: ConnectionsSheetState) {
        if (previousValue.manifest != manifest) savedStateHandle.set(KEY_MANIFEST, manifest)
    }

    companion object {
        private const val KEY_MANIFEST = "key_manifest"
    }
}

/**
 *  Class containing all side effects intended to be run by the view.
 *
 *  Mostly one-off actions to be executed by the view will be instances of ViewEffect.
 */
internal sealed class ConnectionsSheetViewEffect {

    /**
     * Open the AuthFlow.
     */
    data class OpenAuthFlowWithUrl(
        val url: String
    ) : ConnectionsSheetViewEffect()

    /**
     * Finish [ConnectionsSheetActivity] with a given [ConnectionsSheetResult]
     */
    data class FinishWithResult(
        val result: ConnectionsSheetResult
    ) : ConnectionsSheetViewEffect()
}
