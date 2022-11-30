package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState

internal class FlowControllerViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {
    var paymentSelection: PaymentSelection? = null

    var state: PaymentSheetState.Full?
        get() = handle[STATE_KEY]
        set(value) {
            handle[STATE_KEY] = value
        }

    private companion object {
        private const val STATE_KEY = "state"
    }
}
