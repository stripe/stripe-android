package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
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
    init {
        state?.let { restoredState ->
            if (restoredState.savedPaymentMethodSelectionState is SavedPaymentMethodSelectionState.Pending) {
                state = restoredState.copy(
                    savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
                )
            }
        }
    }

    var state: CheckoutControllerState?
        get() = savedStateHandle[STATE_KEY]
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    val stateFlow: StateFlow<CheckoutControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    val session: StateFlow<Session?> =
        stateFlow
            .mapAsStateFlow {
                it?.copy(savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle)
            }
            .mapAsStateFlow {
                it?.asCheckoutSession(
                    paymentOptionFactory,
                    availableExpressButtonTypesFactory,
                )
            }

    override val savedPaymentMethodSelectionState: StateFlow<SavedPaymentMethodSelectionState> =
        stateFlow.mapAsStateFlow {
            it?.savedPaymentMethodSelectionState ?: SavedPaymentMethodSelectionState.Idle
        }

    override val selectionError: StateFlow<ResolvableString?> =
        stateFlow.mapAsStateFlow { it?.selectionError }

    fun tryBeginSavedSelection(): Boolean {
        val current = state ?: return false
        if (current.savedPaymentMethodSelectionState is SavedPaymentMethodSelectionState.Pending) {
            return false
        }

        state = current.copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Pending,
            selectionError = null,
        )
        return true
    }

    fun failSavedSelection(error: Throwable) {
        state = state?.copy(
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
            selectionError = error.stripeErrorMessage(),
        )
    }

    fun finishSavedSelection() {
        val current = state ?: return
        if (current.savedPaymentMethodSelectionState is SavedPaymentMethodSelectionState.Pending) {
            state = current.copy(
                savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
            )
        }
    }

    override val selection: StateFlow<PaymentSelection?> =
        stateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        stateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: Bundle
        get() = state?.previousNewSelections ?: Bundle()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        state = current.withSelection(updatedSelection).copy(selectionError = null)
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
