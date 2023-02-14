package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentSheetSetupFutureUse {
    OnSession,
    OffSession,
}

internal fun StripeIntent.setupFutureUse(): PaymentSheetSetupFutureUse? {
    return when (this) {
        is PaymentIntent -> {
            when (setupFutureUsage) {
                StripeIntent.Usage.OnSession -> PaymentSheetSetupFutureUse.OnSession
                StripeIntent.Usage.OffSession -> PaymentSheetSetupFutureUse.OffSession
                StripeIntent.Usage.OneTime, null -> null
            }
        }
        is SetupIntent -> PaymentSheetSetupFutureUse.OffSession
    }
}
