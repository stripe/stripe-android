package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentSheetMode {
    object Payment : PaymentSheetMode
    object Setup : PaymentSheetMode
}

internal fun StripeIntent.mode(): PaymentSheetMode {
    return when (this) {
        is PaymentIntent -> PaymentSheetMode.Payment
        is SetupIntent -> PaymentSheetMode.Setup
    }
}
