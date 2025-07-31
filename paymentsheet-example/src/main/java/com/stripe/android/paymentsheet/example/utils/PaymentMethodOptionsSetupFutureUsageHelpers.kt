package com.stripe.android.paymentsheet.example.utils

import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.model.PaymentMethod

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun getPMOSFUFromStringMap(
    values: Map<String, String>
): IntentConfiguration.Mode.Payment.PaymentMethodOptions {
    val setupFutureUsageValues = values.mapNotNull { (key, value) ->
        val paymentMethodType = PaymentMethod.Type.fromCode(key)
        val setupFutureUse = when (value) {
            "off_session" -> IntentConfiguration.SetupFutureUse.OffSession
            "on_session" -> IntentConfiguration.SetupFutureUse.OnSession
            "none" -> IntentConfiguration.SetupFutureUse.None
            else -> null
        }

        if (paymentMethodType != null && setupFutureUse != null) {
            paymentMethodType to setupFutureUse
        } else {
            null
        }
    }.toMap()

    return IntentConfiguration.Mode.Payment.PaymentMethodOptions(
        setupFutureUsageValues = setupFutureUsageValues
    )
}

internal fun stringValueToMap(value: String): Map<String, String>? {
    return value.split(",")
        .mapNotNull { entry ->
            entry.split(":").takeIf { it.size == 2 }?.let { pair ->
                pair[0] to pair[1]
            }
        }.toMap().ifEmpty { null }
}
