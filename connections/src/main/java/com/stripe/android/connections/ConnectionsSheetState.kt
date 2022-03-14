package com.stripe.android.connections

import com.stripe.android.connections.model.LinkAccountSessionManifest

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class ConnectionsSheetState(
    val manifest: LinkAccountSessionManifest? = null,
    val authFlowActive: Boolean = false
)

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
