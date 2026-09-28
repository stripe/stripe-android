package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.elements.ece.AvailableExpressButtonTypesFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.FlowToStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
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
            if (value == null) {
                sessionProjection.clear()
            }
        }

    val stateFlow: StateFlow<CheckoutControllerState?> =
        savedStateHandle.getStateFlow(STATE_KEY, null)

    private val sessionProjection = CheckoutSessionProjection(
        sourceStateFlow = stateFlow,
        paymentOptionFactory = paymentOptionFactory,
        availableExpressButtonTypesFactory = availableExpressButtonTypesFactory,
    )

    val session: StateFlow<Session?> = sessionProjection.stateFlow

    override val selection: StateFlow<PaymentSelection?> =
        stateFlow.mapAsStateFlow { it?.paymentSelection }

    override val temporarySelection: StateFlow<String?> =
        stateFlow.mapAsStateFlow { it?.temporarySelection }

    override val previousNewSelections: Bundle
        get() = state?.previousNewSelections ?: Bundle()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        val current = requireState(operation = "setSelection") ?: return
        updatedSelection?.hasAcknowledgedSepaMandate = true
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

@OptIn(CheckoutSessionPreview::class)
private class CheckoutSessionProjection(
    private val sourceStateFlow: StateFlow<CheckoutControllerState?>,
    private val paymentOptionFactory: CheckoutPaymentOptionDisplayDataFactory,
    private val availableExpressButtonTypesFactory: AvailableExpressButtonTypesFactory,
) {
    // `.value` reads must not advance the last option seen by an active collector.
    @Volatile
    private var latestCollectedPaymentOption: PreviousPaymentOption? = null

    @Volatile
    private var latestValuePaymentOption: PreviousPaymentOption? = null

    val stateFlow: StateFlow<Session?> = createStateFlow()

    @Suppress("DEPRECATION")
    private fun createStateFlow(): StateFlow<Session?> = FlowToStateFlow(
        flow = flow {
            var previousPaymentOption: PreviousPaymentOption? = null
            sourceStateFlow.collect { state ->
                val session = state.toCheckoutSession(
                    previousPaymentOptions = listOf(
                        previousPaymentOption,
                        latestCollectedPaymentOption,
                        latestValuePaymentOption,
                    ),
                )
                previousPaymentOption = state?.let {
                    PreviousPaymentOption(
                        selection = it.paymentSelection,
                        displayData = session?.paymentOption,
                    )
                }
                recordCollectedPaymentOption(state, previousPaymentOption)
                emit(session)
            }
        },
        produceValue = ::currentValue,
    )

    @Synchronized
    private fun currentValue(): Session? {
        val currentState = sourceStateFlow.value
        val session = currentState.toCheckoutSession(
            previousPaymentOptions = listOf(
                latestCollectedPaymentOption,
                latestValuePaymentOption,
            ),
        )
        latestValuePaymentOption = currentState?.let {
            PreviousPaymentOption(
                selection = it.paymentSelection,
                displayData = session?.paymentOption,
            )
        }
        return session
    }

    @Synchronized
    fun clear() {
        latestCollectedPaymentOption = null
        latestValuePaymentOption = null
    }

    @Synchronized
    private fun recordCollectedPaymentOption(
        state: CheckoutControllerState?,
        paymentOption: PreviousPaymentOption?,
    ) {
        if (sourceStateFlow.value === state) {
            latestCollectedPaymentOption = paymentOption
        }
    }

    private fun CheckoutControllerState?.toCheckoutSession(
        previousPaymentOptions: List<PreviousPaymentOption?>,
    ): Session? = this?.asCheckoutSession(
        paymentOptionFactory = paymentOptionFactory.reusing(previousPaymentOptions),
        availableExpressButtonTypesFactory = availableExpressButtonTypesFactory,
    )

    private fun CheckoutPaymentOptionDisplayDataFactory.reusing(
        previousPaymentOptions: List<PreviousPaymentOption?>,
    ): CheckoutPaymentOptionDisplayDataFactory =
        CheckoutPaymentOptionDisplayDataFactory { selection, paymentMethodMetadata ->
            val displayData = create(selection, paymentMethodMetadata)
            previousPaymentOptions.firstOrNull { previousPaymentOption ->
                previousPaymentOption != null &&
                    previousPaymentOption.selection == selection &&
                    previousPaymentOption.displayData.hasSameVisibleContentAs(displayData)
            }?.displayData ?: displayData
        }

    private data class PreviousPaymentOption(
        val selection: PaymentSelection?,
        val displayData: PaymentOptionDisplayData?,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun PaymentOptionDisplayData?.hasSameVisibleContentAs(
    other: PaymentOptionDisplayData?,
): Boolean = when {
    this === other -> true
    this == null || other == null -> false
    else ->
        label == other.label &&
            billingDetails == other.billingDetails &&
            paymentMethodType == other.paymentMethodType &&
            mandateText == other.mandateText
}
