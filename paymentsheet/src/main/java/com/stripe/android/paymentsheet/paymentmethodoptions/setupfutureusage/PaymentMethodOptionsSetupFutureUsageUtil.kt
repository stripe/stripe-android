package com.stripe.android.paymentsheet.paymentmethodoptions.setupfutureusage

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration.SetupFutureUse

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions.toJsonObjectString(): String {
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
