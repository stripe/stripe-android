package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FlowControllerViewModel(
    application: Application,
    val handle: SavedStateHandle,
    statusBarColor: () -> Int?,
) : AndroidViewModel(application) {

    val flowControllerStateComponent: FlowControllerStateComponent =
        DaggerFlowControllerStateComponent
            .builder()
            .application(application)
            .statusBarColor(statusBarColor)
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

    class Factory(
        private val statusBarColor: () -> Int?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return FlowControllerViewModel(
                application = extras.requireApplication(),
                handle = extras.createSavedStateHandle(),
                statusBarColor = statusBarColor,
            ) as T
        }
    }

    private companion object {
        private const val STATE_KEY = "state"
    }
}
