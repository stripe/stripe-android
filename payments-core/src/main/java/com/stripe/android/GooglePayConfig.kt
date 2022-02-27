package com.stripe.android

import android.content.Context
import com.stripe.android.core.ApiKeyValidator
import com.stripe.android.core.ApiVersion
import org.json.JSONException
import org.json.JSONObject

/**
 * Configuration settings for Google Pay's `TokenizationSpecification`.
 */
class GooglePayConfig @JvmOverloads constructor(
    publishableKey: String,
    private val connectedAccountId: String? = null
) {
    private val validPublishableKey: String = ApiKeyValidator.get().requireValid(publishableKey)
    private val apiVersion: String = ApiVersion.get().code

    /**
     * @return a [JSONObject] representing a [Google Pay TokenizationSpecification](https://developers.google.com/pay/api/android/reference/object#gateway)
     * configured for Stripe
     */
    val tokenizationSpecification: JSONObject
        @Throws(JSONException::class)
        get() = JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put(
                "parameters",
                JSONObject()
                    .put("gateway", "stripe")
                    .put("stripe:version", apiVersion)
                    .put("stripe:publishableKey", key)
            )

    private val key: String
        get() = connectedAccountId?.let { "$validPublishableKey/$it" } ?: validPublishableKey

    /**
     * Instantiate with [PaymentConfiguration].
     * [PaymentConfiguration] must be initialized.
     */
    constructor(context: Context) : this(
        PaymentConfiguration.getInstance(context)
    )

    private constructor(
        paymentConfiguration: PaymentConfiguration
    ) : this(
        publishableKey = paymentConfiguration.publishableKey,
        connectedAccountId = paymentConfiguration.stripeAccountId
    )
}
