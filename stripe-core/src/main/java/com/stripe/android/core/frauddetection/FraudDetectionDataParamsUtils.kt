package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo

/**
 * Utility class for adding fraud detection data to API params
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FraudDetectionDataParamsUtils {
    fun addFraudDetectionData(
        params: Map<String, *>,
        fraudDetectionData: FraudDetectionData?
    ): Map<String, *> {
        return setOf("source_data", "payment_method_data")
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
