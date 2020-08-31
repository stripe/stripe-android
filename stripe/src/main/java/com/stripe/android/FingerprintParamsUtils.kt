package com.stripe.android

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA

/**
 * Utility class for adding fingerprint data to API params
 */
internal class FingerprintParamsUtils {
    internal fun addFingerprintData(
        params: Map<String, *>,
        fingerprintData: FingerprintData?
    ): Map<String, *> {
        return setOf(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA, PARAM_PAYMENT_METHOD_DATA)
            .firstOrNull { key ->
                params.containsKey(key)
            }?.let { key ->
                addFingerprintData(
                    params,
                    key,
                    fingerprintData
                )
            } ?: params
    }

    private fun addFingerprintData(
        stripeIntentParams: Map<String, *>,
        key: String,
        fingerprintData: FingerprintData?
    ): Map<String, *> {
        return (stripeIntentParams[key] as? Map<*, *>)?.let {
            stripeIntentParams.plus(
                mapOf(
                    key to it.plus(
                        fingerprintData?.params.orEmpty()
                    )
                )
            )
        } ?: stripeIntentParams
    }
}
