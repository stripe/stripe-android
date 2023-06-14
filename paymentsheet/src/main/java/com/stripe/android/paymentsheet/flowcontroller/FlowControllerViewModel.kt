package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState

internal class FlowControllerViewModel(
    application: Application,
    private val handle: SavedStateHandle,
) : AndroidViewModel(application) {

    val flowControllerStateComponent: FlowControllerStateComponent =
        DaggerFlowControllerStateComponent
            .builder()
            .appContext(application)
            .flowControllerViewModel(this)
            .build()

    var paymentSelection: PaymentSelection? = null

    // Used to determine if we need to reload the flow controller configuration.
    var previousConfigureRequest: FlowControllerConfigurationHandler.ConfigureRequest? = null

    var state: PaymentSheetState.Full?
        get() = handle[STATE_KEY]
        set(value) {
            handle[STATE_KEY] = value
        }

    fun setIntentConfirmationType(type: String?) {
        handle[DEFERRED_INTENT_CONFIRMATION_TYPE] = type
    }

    fun retrieveAndClearIntentConfirmationType(): String? {
        return handle.remove(DEFERRED_INTENT_CONFIRMATION_TYPE)
    }

    private companion object {
        private const val STATE_KEY = "state"
        private const val DEFERRED_INTENT_CONFIRMATION_TYPE = "DEFERRED_INTENT_CONFIRMATION_TYPE"
    }
}
