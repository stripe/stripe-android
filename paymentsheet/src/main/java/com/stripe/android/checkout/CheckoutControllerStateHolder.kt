package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns [CheckoutController]'s [CheckoutControllerState], persisting it in [SavedStateHandle] so it
 * survives process death, and derives the observable [checkoutSession] from it. Kept separate from
 * the controller so [CheckoutStateLoader] can commit loaded state directly rather than reaching back
 * into the controller.
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var state: CheckoutControllerState?
        get() = savedStateHandle[STATE_KEY]
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val stateFlow: StateFlow<CheckoutControllerState?>
        get() = savedStateHandle.getStateFlow(STATE_KEY, null)

    val checkoutSession: StateFlow<CheckoutSession?> =
        stateFlow.mapAsStateFlow { it?.asCheckoutSession() }

    companion object {
        const val STATE_KEY = "CheckoutController_InternalState"
    }
}
