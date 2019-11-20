package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA

/**
 * Utility class for static functions useful for networking and data transfer.
 */
internal class StripeNetworkUtils @VisibleForTesting constructor(
    private val uidParamsFactory: UidParamsFactory
) {
    constructor(context: Context) : this(
        UidParamsFactory.create(context)
    )

    /**
     * A utility function to map the fields of a [Card] object into a [Map] we
     * can use in network communications.
     *
     * @param card the [Card] to be read
     * @return a [Map] containing the appropriate values read from the card
     */
    fun createCardTokenParams(card: Card): Map<String, Any> {
        return card.toParamMap()
            // We store the logging items in this field, which is extracted from the parameters
            // sent to the API.
            .plus(AnalyticsDataFactory.FIELD_PRODUCT_USAGE to card.loggingTokens)
            .plus(uidParamsFactory.createParams())
    }

    internal fun paramsWithUid(intentParams: Map<String, *>): Map<String, *> {
        return when {
            intentParams.containsKey(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA) ->
                paramsWithUid(
                    intentParams,
                    ConfirmPaymentIntentParams.PARAM_SOURCE_DATA
                )
            intentParams.containsKey(PARAM_PAYMENT_METHOD_DATA) ->
                paramsWithUid(
                    intentParams,
                    PARAM_PAYMENT_METHOD_DATA
                )
            else -> intentParams
        }
    }

    private fun paramsWithUid(stripeIntentParams: Map<String, *>, key: String): Map<String, *> {
        val data = stripeIntentParams[key]
        return if (data is Map<*, *>) {
            val mutableParams = stripeIntentParams.toMutableMap()
            mutableParams[key] = data.plus(uidParamsFactory.createParams())
            mutableParams.toMap()
        } else {
            stripeIntentParams
        }
    }

    fun createUidParams(): Map<String, String> {
        return uidParamsFactory.createParams()
    }
}
