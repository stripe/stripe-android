package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmPaymentIntentParams

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

    fun addUidToConfirmPaymentIntentParams(confirmPaymentIntentParams: Map<String, *>) {
        val sourceData =
            confirmPaymentIntentParams[ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA]
        if (sourceData is MutableMap<*, *>) {
            (sourceData as MutableMap<String, Any>)
                .putAll(uidParamsFactory.createParams())
        } else {
            val paymentMethodData =
                confirmPaymentIntentParams[ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA]
            if (paymentMethodData is MutableMap<*, *>) {
                (paymentMethodData as MutableMap<String, Any>)
                    .putAll(uidParamsFactory.createParams())
            }
        }
    }

    fun createUidParams(): Map<String, String> {
        return uidParamsFactory.createParams()
    }
}
