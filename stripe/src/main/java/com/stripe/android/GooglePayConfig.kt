package com.stripe.android

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

class GooglePayConfig @JvmOverloads constructor(
    publishableKey: String,
    private val connectedAccountId: String? = null
) {
    private val publishableKey: String = ApiKeyValidator.get().requireValid(publishableKey)
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
        get() = connectedAccountId?.let { "$publishableKey/$it" } ?: publishableKey

    /**
     * Instantiate with [PaymentConfiguration] and optional Connect Account Id.
     * [PaymentConfiguration] must be initialized.
     */
    @JvmOverloads
    constructor(context: Context, connectedAccountId: String? = null) : this(
        PaymentConfiguration.getInstance(context).publishableKey,
        connectedAccountId
    )
}
