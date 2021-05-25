package com.stripe.android.networking

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA

/**
 * Utility class for adding fraud detection data to API params
 */
internal class FraudDetectionDataParamsUtils {
    internal fun addFraudDetectionData(
        params: Map<String, *>,
        fraudDetectionData: FraudDetectionData?
    ): Map<String, *> {
        return setOf(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA, PARAM_PAYMENT_METHOD_DATA)
            .firstOrNull { key ->
                params.containsKey(key)
            }?.let { key ->
                addFraudDetectionData(
                    params,
                    key,
                    fraudDetectionData
                )
            } ?: params
    }

    private fun addFraudDetectionData(
        stripeIntentParams: Map<String, *>,
        key: String,
        fraudDetectionData: FraudDetectionData?
    ): Map<String, *> {
        return (stripeIntentParams[key] as? Map<*, *>)?.let {
            stripeIntentParams.plus(
                mapOf(
                    key to it.plus(
                        fraudDetectionData?.params.orEmpty()
                    )
                )
            )
        } ?: stripeIntentParams
    }
}
