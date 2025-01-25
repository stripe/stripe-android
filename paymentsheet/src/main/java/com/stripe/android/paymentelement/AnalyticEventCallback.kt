package com.stripe.android.paymentelement

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

abstract class AnalyticEvent internal constructor() {

    @Override
    override fun toString(): String {
        return this::class.java.simpleName
    }

    // Sheet is presented
    class PresentPaymentSheet internal constructor() : AnalyticEvent()

    // Selected a different payment method type
    class SelectedPaymentMethodType internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // Payment method form for was displayed
    class DisplayedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User interacted with a payment method form
    class StartedInteractionWithPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // All mandatory fields for the payment method form have been completed
    class CompletedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User tapped on the confirm button
    class TappedConfirmButton internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User selected a saved payment method
    class SelectedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User removed a saved payment method
    class RemovedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()
}

internal object AnalyticsManager {
    private val _events = MutableSharedFlow<AnalyticEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: AnalyticEvent) {
        _events.emit(event)
    }
}
