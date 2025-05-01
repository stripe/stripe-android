package com.stripe.android.paymentsheet.example.utils

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun getPMOSFUFromStringMap(
    values: Map<String, String>
): PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions {
    val setupFutureUsageValues = values.mapNotNull { (key, value) ->
        val paymentMethodType = PaymentMethod.Type.fromCode(key)
        val setupFutureUse = when (value) {
            "off_session" -> PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
            "on_session" -> PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession
            "none" -> PaymentSheet.IntentConfiguration.SetupFutureUse.None
            else -> null
        }

        if (paymentMethodType != null && setupFutureUse != null) {
            paymentMethodType to setupFutureUse
        } else {
            null
        }
    }.toMap()

    return PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
        setupFutureUsageValues = setupFutureUsageValues
    )
}

fun stringValueToMap(value: String): Map<String, String>? {
    return value.split(",")
        .mapNotNull { entry ->
            entry.split(":").takeIf { it.size == 2 }?.let { pair ->
                pair[0] to pair[1]
            }
        }.toMap().ifEmpty { null }
}
