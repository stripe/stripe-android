package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.PreviousNewSelections
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns [CheckoutController]'s [CheckoutControllerState] — the single source of truth for the
 * controller — persisting it in [SavedStateHandle] so it survives process death. All observable
 * projections (e.g. [session]) are derived from the one [stateFlow]. Kept separate from the
 * controller so [CheckoutStateLoader] can commit loaded state directly rather than reaching back
 * into the controller.
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
    private val availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
) : EmbeddedSelectionHolder {
    var state: CheckoutControllerState?
        get() = savedStateHandle[STATE_KEY]
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val stateFlow: StateFlow<CheckoutControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    val session: StateFlow<Session?> =
        stateFlow.mapAsStateFlow {
            it?.asCheckoutSession(
                paymentOptionFactory,
                availableExpressButtonTypesFactory,
            )
        }

    override val selection: StateFlow<PaymentSelection?> =
        stateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        stateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: PreviousNewSelections
        get() = state?.previousNewSelections ?: PreviousNewSelections.empty

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        state = current.copy(
            paymentSelection = updatedSelection,
            previousNewSelections = current.previousNewSelections.updatedWith(updatedSelection),
        )
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) {
        val current = requireState(operation = "setTemporarySelection") ?: return
        state = current.copy(temporarySelection = code)
    }

    override fun setPreviousNewSelections(selections: PreviousNewSelections) {
        val current = requireState(operation = "setPreviousNewSelections") ?: return
        state = current.copy(
            previousNewSelections = current.previousNewSelections.mergedWith(selections),
        )
    }

    override fun clearPreviousNewSelections() {
        val current = requireState(operation = "clearPreviousNewSelections") ?: return
        state = current.copy(previousNewSelections = PreviousNewSelections.empty)
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections[code]
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
