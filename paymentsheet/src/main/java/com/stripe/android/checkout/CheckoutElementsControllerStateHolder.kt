package com.stripe.android.checkout

import android.os.Bundle
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutElementsController.Session
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
 * Owns [CheckoutElementsController]'s [CheckoutElementsControllerState] — the single source of truth for the
 * controller — persisting it in [SavedStateHandle] so it survives process death. All observable
 * projections (e.g. [session]) are derived from the one [stateFlow]. Kept separate from the
 * controller so [CheckoutStateLoader] can commit loaded state directly rather than reaching back
 * into the controller.
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutElementsControllerStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val errorReporter: ErrorReporter,
    private val paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
    private val availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
) : EmbeddedSelectionHolder {
    private var loadingGeneration = 0L

    val stateFlow: StateFlow<CheckoutElementsControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    val loadedStateFlow: StateFlow<CheckoutElementsControllerLoadedState?> =
        stateFlow.mapAsStateFlow { (it as? CheckoutElementsControllerState.Loaded)?.state }

    /**
     * Snapshot reads are safe from any thread. Writes and read-modify-write operations require Main.
     */
    @get:AnyThread
    @set:MainThread
    var state: CheckoutElementsControllerState?
        get() = stateFlow.value
        set(value) {
            savedStateHandle[STATE_KEY] = value
        }

    @get:AnyThread
    @set:MainThread
    var loadedState: CheckoutElementsControllerLoadedState?
        get() = loadedStateFlow.value
        set(value) {
            if (state is CheckoutElementsControllerState.Loading) return
            state = value?.let(CheckoutElementsControllerState::Loaded)
        }

    init {
        if (state is CheckoutElementsControllerState.Loading) {
            state = null
        }
        // A restored Pending has no mutation left to finish it, so reset it to avoid loading forever.
        loadedState?.let { restoredState ->
            if (restoredState.savedPaymentMethodSelectionState is SavedPaymentMethodSelectionState.Pending) {
                loadedState = restoredState.copy(
                    savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
                )
            }
        }
    }

    @MainThread
    fun beginLoading(configuration: CheckoutElementsController.Configuration.State): Long {
        loadingGeneration += 1
        state = CheckoutElementsControllerState.Loading(CheckoutElementsControllerLoadingState(configuration))
        return loadingGeneration
    }

    @MainThread
    fun completeLoading(generation: Long, loaded: CheckoutElementsControllerLoadedState): Boolean {
        if (generation != loadingGeneration || state !is CheckoutElementsControllerState.Loading) return false
        state = CheckoutElementsControllerState.Loaded(loaded)
        return true
    }

    @MainThread
    fun clearLoading(generation: Long) {
        if (generation == loadingGeneration && state is CheckoutElementsControllerState.Loading) {
            state = null
        }
    }

    val session: StateFlow<Session?> =
        loadedStateFlow.mapAsStateFlow {
            it?.asCheckoutSession(
                paymentOptionFactory,
                availableExpressButtonTypesFactory,
            )
        }

    override val selection: StateFlow<PaymentSelection?> =
        loadedStateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        loadedStateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: Bundle
        get() = loadedState?.previousNewSelections ?: Bundle()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        loadedState = current.commitSelection(updatedSelection)
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) {
        val current = requireState(operation = "setTemporarySelection") ?: return
        loadedState = current.copy(temporarySelection = code)
    }

    override fun setPreviousNewSelections(bundle: Bundle) {
        val current = requireState(operation = "setPreviousNewSelections") ?: return
        val previousNewSelections = Bundle(current.previousNewSelections).apply {
            putAll(bundle)
        }
        loadedState = current.copy(previousNewSelections = previousNewSelections)
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections.previousNewSelection(code)
    }

    /**
     * The selection lives on [CheckoutElementsControllerLoadedState], so the mutators can only act once
     * [CheckoutStateLoader] has committed a state. A call before then is a programming error (a
     * mis-ordered selection write); report it and no-op rather than silently dropping the value.
     */
    private fun requireState(operation: String): CheckoutElementsControllerLoadedState? {
        return loadedState ?: run {
            errorReporter.report(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SELECTION_SET_BEFORE_LOAD,
                additionalNonPiiParams = mapOf("operation" to operation),
            )
            null
        }
    }

    companion object {
        const val STATE_KEY = "CheckoutElementsController_InternalState"
    }
}
