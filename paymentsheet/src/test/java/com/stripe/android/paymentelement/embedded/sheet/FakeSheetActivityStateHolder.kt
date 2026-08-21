package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

internal class FakeSheetActivityStateHolder : SheetActivityStateHolder {
    val selectionTurbine = Turbine<PaymentSelection.Saved?>()

    override val state = MutableStateFlow(
        SheetActivityStateHolder.State(
            primaryButtonLabel = "".resolvableString,
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            shouldDisplayLockIcon = true,
            savedPaymentSelectionToConfirm = null,
        )
    )

    val resultTurbine = Turbine<EmbeddedActivityResult>()
    val updateErrorTurbine = Turbine<ResolvableString?>()
    val updateProcessingTurbine = Turbine<Boolean>()

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

    override fun updateProcessing(isProcessing: Boolean) {
        updateProcessingTurbine.add(isProcessing)
    }

    override fun setResult(result: EmbeddedActivityResult) {
        resultTurbine.add(result)
    }

    override fun updateSavedPaymentSelectionToConfirm(selection: PaymentSelection.Saved?) {
        selectionTurbine.add(selection)
    }

    fun validate() {
        resultTurbine.ensureAllEventsConsumed()
        updateErrorTurbine.ensureAllEventsConsumed()
        updateProcessingTurbine.ensureAllEventsConsumed()
    }
}
