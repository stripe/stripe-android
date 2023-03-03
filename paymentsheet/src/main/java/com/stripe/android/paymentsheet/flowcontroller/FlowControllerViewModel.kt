package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState

internal class FlowControllerViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {

    var initializationMode: PaymentSheet.InitializationMode? = null

    var paymentSelection: PaymentSelection? = null

    // Used to determine if we need to reload the flow controller configuration.
    var previousElementsSessionParams: ElementsSessionParams? = null

    var state: PaymentSheetState.Full?
        get() = handle[STATE_KEY]
        set(value) {
            handle[STATE_KEY] = value
        }

    private companion object {
        private const val STATE_KEY = "state"
    }
}
