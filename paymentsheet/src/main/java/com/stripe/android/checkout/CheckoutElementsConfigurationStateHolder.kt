package com.stripe.android.checkout

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class CheckoutElementsConfigurationStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    private val stateFlow: StateFlow<CheckoutElementsController.Configuration.State?> =
        savedStateHandle.getStateFlow(CONFIGURATION_KEY, null)

    @get:AnyThread
    @set:MainThread
    var state: CheckoutElementsController.Configuration.State?
        get() = stateFlow.value
        set(value) {
            savedStateHandle[CONFIGURATION_KEY] = value
        }

    private companion object {
        const val CONFIGURATION_KEY = "CheckoutElementsController_ConfigurationState"
    }
}
