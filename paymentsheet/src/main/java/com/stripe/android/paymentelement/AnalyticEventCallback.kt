package com.stripe.android.paymentelement

import dev.drewhamilton.poko.Poko
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
    class PresentedSheet internal constructor() : AnalyticEvent()

    // Selected a different payment method type
    @Poko
    class SelectedPaymentMethodType internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // Payment method form for was displayed
    @Poko
    class DisplayedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User interacted with a payment method form
    @Poko
    class StartedInteractionWithPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // All mandatory fields for the payment method form have been completed
    @Poko
    class CompletedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User tapped on the confirm button
    @Poko
    class TappedConfirmButton internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User selected a saved payment method
    @Poko
    class SelectedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User removed a saved payment method
    @Poko
    class RemovedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()
}

internal object AnalyticsManager {
    private val _events = MutableSharedFlow<AnalyticEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: AnalyticEvent) {
        _events.emit(event)
    }
}
