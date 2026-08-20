package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class FakeSheetActivityStateHolder(
    initialState: SheetActivityStateHolder.State = SheetActivityStateHolder.State(
        primaryButtonLabel = "".resolvableString,
        isEnabled = false,
        processingState = PrimaryButtonProcessingState.Idle(null),
        isProcessing = false,
        shouldDisplayLockIcon = true,
    ),
) : SheetActivityStateHolder {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<SheetActivityStateHolder.State> = _state.asStateFlow()

    fun updateState(transform: (SheetActivityStateHolder.State) -> SheetActivityStateHolder.State) {
        _state.update(transform)
    }

    val resultTurbine = Turbine<EmbeddedActivityResult>()
    val updateErrorTurbine = Turbine<ResolvableString?>()

    override val result: SharedFlow<EmbeddedActivityResult> = MutableSharedFlow<EmbeddedActivityResult>()

    override fun updateMandate(mandateText: ResolvableString?) {
        error("This should never be called!")
    }

    override fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        error("This should never be called!")
    }

    override fun updateError(error: ResolvableString?) {
        updateErrorTurbine.add(error)
    }

    override fun setResult(result: EmbeddedActivityResult) {
        resultTurbine.add(result)
    }

    fun validate() {
        resultTurbine.ensureAllEventsConsumed()
        updateErrorTurbine.ensureAllEventsConsumed()
    }
}
