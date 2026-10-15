package com.stripe.android.elements

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persists shared controller state. Element adapters derive their UI from this one state flow. */
@Singleton
internal class ElementsControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    val stateFlow: StateFlow<ElementsControllerState?> = savedStateHandle.getStateFlow(STATE_KEY, null)

    @get:AnyThread
    @set:MainThread
    var state: ElementsControllerState?
        get() = stateFlow.value
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val session: StateFlow<ElementsController.Session?> = stateFlow.mapAsStateFlow {
        it?.let { ElementsController.Session(intent = it.paymentMethodMetadata.stripeIntent) }
    }

    private companion object {
        const val STATE_KEY = "elements.controller_state"
    }
}
