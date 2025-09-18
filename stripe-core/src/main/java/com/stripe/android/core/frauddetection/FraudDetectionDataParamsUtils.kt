package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo

/**
 * Utility class for adding fraud detection data to API params
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FraudDetectionDataParamsUtils {
    fun Map<String, *>.addFraudDetectionData(
        fraudDetectionData: FraudDetectionData?
    ): Map<String, *> {
        return setOf("source_data", "payment_method_data")
            .firstOrNull { key ->
                this.containsKey(key)
            }?.let { key ->
                addFraudDetectionData(
                    this,
                    key,
                    fraudDetectionData
                )
            } ?: this
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
