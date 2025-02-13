package com.stripe.android.paymentelement

import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Called when an analytic event is emitted.
 *
 * These events are intended to be used for analytics purposes ONLY.
 *
 * @param event the [AnalyticEvent] that was emitted
 */
@ExperimentalAnalyticEventCallbackApi
fun interface AnalyticEventCallback {
    fun onEvent(event: AnalyticEvent)
}

@ExperimentalAnalyticEventCallbackApi
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

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal object AnalyticManager {
    private val _events = Channel<AnalyticEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun produceEvent(event: AnalyticEvent) {
        _events.send(event)
    }
}
