package com.stripe.android.paymentsheet.navigation

internal sealed interface TransitionTarget {

    object SelectSavedPaymentMethods : TransitionTarget

    object AddAnotherPaymentMethod : TransitionTarget

    object AddFirstPaymentMethod : TransitionTarget
}
