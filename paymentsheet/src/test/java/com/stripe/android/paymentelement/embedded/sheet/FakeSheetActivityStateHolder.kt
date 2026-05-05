package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeSheetActivityStateHolder : SheetActivityStateHolder {
    val selectionTurbine = Turbine<PaymentSelection.Saved?>()

    override val state: StateFlow<SheetActivityStateHolder.State>
        get() = stateFlowOf(
            SheetActivityStateHolder.State(
                primaryButtonLabel = "".resolvableString,
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Idle(null),
                isProcessing = false,
                shouldDisplayLockIcon = true,
                savedPaymentSelectionToConfirm = null,
            )
        )

    val resultTurbine = Turbine<FormResult>()

    override val result: SharedFlow<FormResult> = MutableSharedFlow<FormResult>()

    override fun updateMandate(mandateText: ResolvableString?) {
        error("This should never be called!")
    }

    override fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        error("This should never be called!")
    }

    override fun updateError(error: ResolvableString?) {
        error("This should never be called!")
    }

    override fun setResult(result: FormResult) {
        resultTurbine.add(result)
    }

    override fun updateSavedPaymentSelectionToConfirm(selection: PaymentSelection.Saved?) {
        selectionTurbine.add(selection)
    }

    fun validate() {
        resultTurbine.ensureAllEventsConsumed()
    }
}
