package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA

/**
 * Utility class for adding fingerprint data to API params
 */
internal class FingerprintParamsUtils @VisibleForTesting constructor(
    private val apiFingerprintParamsFactory: ApiFingerprintParamsFactory
) {
    constructor(context: Context) : this(
        ApiFingerprintParamsFactory(context)
    )

    internal fun addFingerprintData(
        params: Map<String, *>,
        fingerprintGuid: String?
    ): Map<String, *> {
        return setOf(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA, PARAM_PAYMENT_METHOD_DATA)
            .firstOrNull { key ->
                params.containsKey(key)
            }?.let { key ->
                addFingerprintData(
                    params,
                    key,
                    fingerprintGuid
                )
            } ?: params
    }

    private fun addFingerprintData(
        stripeIntentParams: Map<String, *>,
        key: String,
        fingerprintGuid: String?
    ): Map<String, *> {
        return (stripeIntentParams[key] as? Map<*, *>)?.let {
            stripeIntentParams.plus(
                mapOf(key to it.plus(
                    apiFingerprintParamsFactory.createParams(fingerprintGuid)
                ))
            )
        } ?: stripeIntentParams
    }
}
