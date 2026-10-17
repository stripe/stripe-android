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

private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
internal fun MutableMap<String, Any>.putNonEmptySfu(sfu: ConfirmPaymentIntentParams.SetupFutureUsage?) {
    sfu.takeIf {
        // Empty values are an attempt to unset a parameter;
        // however, setup_future_usage cannot be unset.
        it != null && it != ConfirmPaymentIntentParams.SetupFutureUsage.Blank
    }?.let {
        put(PARAM_SETUP_FUTURE_USAGE, it.code)
    }
}
