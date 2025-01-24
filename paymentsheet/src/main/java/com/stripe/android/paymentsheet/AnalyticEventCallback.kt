package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Called when an analytic event is emitted.
 *
 * This should only be used for analytic purposes. It is not intended for general use.
 *
 * @param event the [AnalyticEvent] that was emitted
 */
fun interface AnalyticEventCallback {
    fun onEvent(event: AnalyticEvent)
}

sealed class AnalyticEvent {
    // Sheet is presented
    data object PresentPaymentSheet : AnalyticEvent()

    // Selected a different payment method type
    data class SelectedPaymentMethodType(val paymentMethodType: String) : AnalyticEvent()

    // Payment method form for was displayed
    data class DisplayedPaymentMethodForm(val paymentMethodType: String) : AnalyticEvent()

    // User interacted with a payment method form
    data class StartedInteractionWithPaymentMethodForm(val paymentMethodType: String) : AnalyticEvent()

    // All mandatory fields for the payment method form have been completed
    data class CompletedPaymentMethodForm(val paymentMethodType: String) : AnalyticEvent()

    // User tapped on the confirm button
    data class TappedConfirmButton(val paymentMethodType: String) : AnalyticEvent()

    // User selected a saved payment method
    data class SelectedSavedPaymentMethod(val paymentMethodType: String) : AnalyticEvent()

    // User removed a saved payment method
    data class RemovedSavedPaymentMethod(val paymentMethodType: String) : AnalyticEvent()
}

internal object AnalyticsManager {
    private val _events = MutableSharedFlow<AnalyticEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: AnalyticEvent) {
        _events.emit(event)
    }
}
