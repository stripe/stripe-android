package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.PaymentSheet
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
    var previousInitializationMode: PaymentSheet.InitializationMode? = null
    var previousConfiguration: PaymentSheet.Configuration? = null

    var state: PaymentSheetState.Full?
        get() = handle[STATE_KEY]
        set(value) {
            handle[STATE_KEY] = value
        }

    fun canSkipLoad(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ): Boolean {
        return previousInitializationMode == initializationMode &&
            previousConfiguration == configuration
    }

    fun storeLastInput(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ) {
        previousInitializationMode = initializationMode
        previousConfiguration = configuration
    }

    private companion object {
        private const val STATE_KEY = "state"
    }
}
