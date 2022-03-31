package com.stripe.android.connections

import com.stripe.android.connections.model.LinkAccountSessionManifest
import java.util.UUID

/**
 *  Class containing all of the data needed to represent the screen.
 */
internal data class ConnectionsSheetState(
    val activityRecreated: Boolean = false,
    val manifest: LinkAccountSessionManifest? = null,
    val authFlowActive: Boolean = false,
    val viewEffects: List<ConnectionsSheetViewEffect> = emptyList()
) {
    operator fun plus(viewEffect: ConnectionsSheetViewEffect): ConnectionsSheetState {
        return copy(viewEffects = viewEffects + viewEffect)
    }

    operator fun minus(viewEffect: ConnectionsSheetViewEffect): ConnectionsSheetState {
        return copy(viewEffects = viewEffects.filterNot { it.id == viewEffect.id })
    }
}

/**
 *  Class containing all side effects intended to be run by the view.
 *
 *  Mostly one-off actions to be executed by the view will be instances of ViewEffect.
 */
internal sealed class ConnectionsSheetViewEffect(
    val id: Long = UUID.randomUUID().mostSignificantBits,
) {

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
