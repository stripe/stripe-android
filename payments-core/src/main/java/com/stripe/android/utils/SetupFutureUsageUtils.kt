package com.stripe.android.utils

import androidx.annotation.RestrictTo
import com.stripe.android.model.ConfirmPaymentIntentParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ConfirmPaymentIntentParams.SetupFutureUsage?.hasIntentToSetup() = when (this) {
    ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
    ConfirmPaymentIntentParams.SetupFutureUsage.OffSession -> true
    ConfirmPaymentIntentParams.SetupFutureUsage.Blank -> false
    ConfirmPaymentIntentParams.SetupFutureUsage.None -> false
    null -> false
}
