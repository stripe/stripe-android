package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

/**
 * Called when an analytic event is emitted.
 *
 * These events are intended to be used for analytics purposes ONLY.
 *
 * @param event the [AnalyticEvent] that was emitted
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAnalyticEventCallbackApi
fun interface AnalyticEventCallback {
    fun onEvent(event: AnalyticEvent)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAnalyticEventCallbackApi
abstract class AnalyticEvent internal constructor() {

    // Sheet is presented
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PresentedSheet internal constructor() : AnalyticEvent() {
        override fun toString(): String =
            javaClass.simpleName

        override fun equals(other: Any?): Boolean =
            other is PresentedSheet

        override fun hashCode(): Int =
            javaClass.hashCode()
    }

    // Selected a different payment method type
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class SelectedPaymentMethodType internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // Payment method form for was displayed
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class DisplayedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User interacted with a payment method form
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class StartedInteractionWithPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // All mandatory fields for the payment method form have been completed
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CompletedPaymentMethodForm internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User tapped on the confirm button
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TappedConfirmButton internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User selected a saved payment method
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class SelectedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()

    // User removed a saved payment method
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class RemovedSavedPaymentMethod internal constructor(val paymentMethodType: String) : AnalyticEvent()
}
