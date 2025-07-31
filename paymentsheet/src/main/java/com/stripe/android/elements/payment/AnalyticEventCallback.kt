package com.stripe.android.elements.payment

import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import dev.drewhamilton.poko.Poko

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

    /**
     * User viewed the payment sheet
     */
    class PresentedSheet internal constructor() : AnalyticEvent() {
        override fun toString(): String =
            javaClass.simpleName

        override fun equals(other: Any?): Boolean =
            other is PresentedSheet

        override fun hashCode(): Int =
            javaClass.hashCode()
    }

    /**
     * User selected a payment method type
     */
    @Poko
    class SelectedPaymentMethodType internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User viewed a payment method form
     */
    @Poko
    class DisplayedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User interacted with a payment method form
     */
    @Poko
    class StartedInteractionWithPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User completed all required payment form fields
     */
    @Poko
    class CompletedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User tapped on the confirm button
     */
    @Poko
    class TappedConfirmButton internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User selected a saved payment method
     */
    @Poko
    class SelectedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()

    /**
     * User removed a saved payment method
     */
    @Poko
    class RemovedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()
}
