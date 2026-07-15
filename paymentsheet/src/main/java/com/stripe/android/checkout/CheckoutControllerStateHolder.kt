package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns [CheckoutController]'s [CheckoutControllerState] — the single source of truth for the
 * controller — persisting it in [SavedStateHandle] so it survives process death. All observable
 * projections (e.g. [checkoutSession]) are derived from the one [stateFlow]. Kept separate from the
 * controller so [CheckoutStateLoader] can commit loaded state directly rather than reaching back
 * into the controller.
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
) : EmbeddedSelectionHolder {
    var state: CheckoutControllerState?
        get() = savedStateHandle[STATE_KEY]
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val stateFlow: StateFlow<CheckoutControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    val checkoutSession: StateFlow<CheckoutSession?> =
        stateFlow.mapAsStateFlow { it?.asCheckoutSession() }

    val configuration: StateFlow<CheckoutController.Configuration.State?> =
        stateFlow.mapAsStateFlow { it?.configuration }

    val paymentMethodMetadata: StateFlow<PaymentMethodMetadata?> =
        stateFlow.mapAsStateFlow { it?.paymentMethodMetadata }

    override val selection: StateFlow<PaymentSelection?> =
        stateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        stateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: Bundle
        get() = state?.previousNewSelections ?: Bundle()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        val previousNewSelections = Bundle(current.previousNewSelections).apply {
            stashNewSelection(updatedSelection)
        }
        state = current.copy(
            paymentSelection = updatedSelection,
            previousNewSelections = previousNewSelections,
        )
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) {
        val current = requireState(operation = "setTemporarySelection") ?: return
        state = current.copy(temporarySelection = code)
    }

    override fun setPreviousNewSelections(bundle: Bundle) {
        val current = requireState(operation = "setPreviousNewSelections") ?: return
        val previousNewSelections = Bundle(current.previousNewSelections).apply {
            putAll(bundle)
        }
        state = current.copy(previousNewSelections = previousNewSelections)
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections.previousNewSelection(code)
    }

    /**
     * The selection lives on [CheckoutControllerState], so the mutators can only act once
     * [CheckoutStateLoader] has committed a state. A call before then is a programming error (a
     * mis-ordered selection write); report it and no-op rather than silently dropping the value.
     */
    private fun requireState(operation: String): CheckoutControllerState? {
        return state ?: run {
            errorReporter.report(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SELECTION_SET_BEFORE_LOAD,
                additionalNonPiiParams = mapOf("operation" to operation),
            )
            null
        }
    }

    companion object {
        const val STATE_KEY = "CheckoutController_InternalState"
    }
}
