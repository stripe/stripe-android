package com.stripe.android.paymentsheet.paymentmethodoptions.setupfutureusage

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.IntentConfiguration.SetupFutureUse
import com.stripe.android.elements.payment.PaymentMethodOptionsSetupFutureUsagePreview

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun IntentConfiguration.Mode.Payment.PaymentMethodOptions.toJsonObjectString(): String {
    val map = setupFutureUsageValues.mapKeys {
        it.key.code
    }.mapValues { entry ->
        mapOf(
            "setup_future_usage" to when (entry.value) {
                SetupFutureUse.OffSession -> "off_session"
                SetupFutureUse.OnSession -> "on_session"
                SetupFutureUse.None -> "none"
            }
        )
    }

    return StripeJsonUtils.mapToJsonObject(map).toString()
}
