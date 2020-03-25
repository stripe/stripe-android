package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA

/**
 * Utility class for static functions useful for networking and data transfer.
 */
internal class StripeNetworkUtils @VisibleForTesting constructor(
    private val apiFingerprintParamsFactory: ApiFingerprintParamsFactory
) {
    constructor(context: Context) : this(
        ApiFingerprintParamsFactory(context)
    )

    internal fun paramsWithUid(
        params: Map<String, *>,
        fingerprintGuid: String?
    ): Map<String, *> {
        return when {
            params.containsKey(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA) ->
                paramsWithUid(
                    params,
                    ConfirmPaymentIntentParams.PARAM_SOURCE_DATA,
                    fingerprintGuid
                )
            params.containsKey(PARAM_PAYMENT_METHOD_DATA) ->
                paramsWithUid(
                    params,
                    PARAM_PAYMENT_METHOD_DATA,
                    fingerprintGuid
                )
            else -> params
        }
    }

    private fun paramsWithUid(
        stripeIntentParams: Map<String, *>,
        key: String,
        fingerprintGuid: String?
    ): Map<String, *> {
        val data = stripeIntentParams[key]
        return if (data is Map<*, *>) {
            val mutableParams = stripeIntentParams.toMutableMap()
            mutableParams[key] = data.plus(
                apiFingerprintParamsFactory.createParams(fingerprintGuid)
            )
            mutableParams.toMap()
        } else {
            stripeIntentParams
        }
    }
}
