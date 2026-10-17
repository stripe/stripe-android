package com.stripe.android.paymentsheet.paymentmethodoptions.setupfutureusage

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration.SetupFutureUse

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions?.toJsonObjectString(
    requireCvcRecollection: Boolean
): String? {
    val map = this?.setupFutureUsageValues?.mapKeys {
        it.key.code
    }?.mapValues { entry ->
        mapOf(
            "setup_future_usage" to when (entry.value) {
                SetupFutureUse.OffSession -> "off_session"
                SetupFutureUse.OnSession -> "on_session"
                SetupFutureUse.None -> "none"
            }
        )
    }?.toMutableMap() ?: mutableMapOf()

    if (requireCvcRecollection) {
        val cardMap = map.getOrPut("card") { emptyMap() }.toMutableMap()
        cardMap["require_cvc_recollection"] = "true"
        map["card"] = cardMap
    }

    return if (map.isEmpty()) {
        null
    } else {
        StripeJsonUtils.mapToJsonObject(map).toString()
    }
}
