package com.stripe.android.paymentsheet.flowcontroller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {
    var paymentSelection: PaymentSelection? = null

    var initData: InitData
        get() = requireNotNull(handle.get(INIT_DATA_KEY))
        set(value) = handle.set(INIT_DATA_KEY, value)

    private companion object {
        private const val INIT_DATA_KEY = "init_data"
    }
}
