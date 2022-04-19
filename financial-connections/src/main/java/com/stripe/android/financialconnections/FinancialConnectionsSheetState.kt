package com.stripe.android.financialconnections

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.model.LinkAccountSessionManifest

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class FinancialConnectionsSheetState(
    val activityRecreated: Boolean = false,
    val manifest: LinkAccountSessionManifest? = null,
    val authFlowActive: Boolean = false
) {

    /**
     * Restores existing persisted fields into the current [FinancialConnectionsSheetState]
     */
    internal fun from(savedStateHandle: SavedStateHandle): FinancialConnectionsSheetState {
        return copy(
            manifest = savedStateHandle.get(KEY_MANIFEST) ?: manifest,
            authFlowActive = savedStateHandle.get(KEY_AUTHFLOW_ACTIVE) ?: authFlowActive,
        )
    }

    /**
     * Saves the persistable fields of this state that changed to the given [SavedStateHandle]
     */
    internal fun to(
        savedStateHandle: SavedStateHandle,
        previousValue: FinancialConnectionsSheetState
    ) {
        if (previousValue.manifest != manifest)
            savedStateHandle.set(KEY_MANIFEST, manifest)
        if (previousValue.authFlowActive != authFlowActive)
            savedStateHandle.set(KEY_AUTHFLOW_ACTIVE, authFlowActive)
    }

    companion object {
        private const val KEY_MANIFEST = "key_manifest"
        private const val KEY_AUTHFLOW_ACTIVE = "key_authflow_active"
    }
}

/**
 *  Class containing all side effects intended to be run by the view.
 *
 *  Mostly one-off actions to be executed by the view will be instances of ViewEffect.
 */
internal sealed class FinancialConnectionsSheetViewEffect {

    /**
     * Open the AuthFlow.
     */
    data class OpenAuthFlowWithUrl(
        val url: String
    ) : FinancialConnectionsSheetViewEffect()

    /**
     * Finish [FinancialConnectionsSheetActivity] with a given [FinancialConnectionsSheetResult]
     */
    data class FinishWithResult(
        val result: FinancialConnectionsSheetResult
    ) : FinancialConnectionsSheetViewEffect()
}
