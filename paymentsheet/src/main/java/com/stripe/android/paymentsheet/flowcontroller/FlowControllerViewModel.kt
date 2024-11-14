package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel(
    application: Application,
    val handle: SavedStateHandle,
) : AndroidViewModel(application) {

    val flowControllerStateComponent: FlowControllerStateComponent =
        DaggerFlowControllerStateComponent
            .builder()
            .application(application)
            .flowControllerViewModel(this)
            .build()

    @Volatile
    var paymentSelection: PaymentSelection? = null

    // Used to determine if we need to reload the flow controller configuration.
    @Volatile
    var previousConfigureRequest: FlowControllerConfigurationHandler.ConfigureRequest? = null

    var state: DefaultFlowController.State?
        get() = handle[STATE_KEY]
        set(value) {
            handle[STATE_KEY] = value
        }

    private val restartSession = SessionSavedStateHandler.attachTo(this, handle)

    fun resetSession() {
        restartSession()
    }

    private companion object {
        private const val STATE_KEY = "state"
    }
}
