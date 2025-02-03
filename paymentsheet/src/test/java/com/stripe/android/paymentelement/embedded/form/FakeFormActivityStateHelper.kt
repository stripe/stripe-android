package com.stripe.android.paymentelement.embedded.form

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeFormActivityStateHelper : FormActivityStateHelper {
    val updateTurbine = Turbine<ConfirmationHandler.State>()

    override val state: StateFlow<FormActivityStateHelper.State>
        get() = stateFlowOf(
            FormActivityStateHelper.State(
                primaryButtonLabel = "".resolvableString,
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Idle(null),
                isProcessing = false
            )
        )

    override fun update(confirmationState: ConfirmationHandler.State) {
        updateTurbine.add(confirmationState)
    }

    fun validate() {
        updateTurbine.ensureAllEventsConsumed()
    }
}
