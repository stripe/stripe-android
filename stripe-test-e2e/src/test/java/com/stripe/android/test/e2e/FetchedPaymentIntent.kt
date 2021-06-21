package com.stripe.android.test.e2e

import com.squareup.moshi.Json

data class FetchedPaymentIntent(
    /**
     * https://stripe.com/docs/api/payment_intents/object#payment_intent_object-amount
     */
    @field:Json(name = "amount") val amount: Int,

    /**
     * https://stripe.com/docs/api/payment_intents/object#payment_intent_object-currency
     */
    @field:Json(name = "currency") val currency: String,

    /**
     * https://stripe.com/docs/api/payment_intents/object#payment_intent_object-on_behalf_of
     */
    @field:Json(name = "on_behalf_of") val onBehalfOf: String
)
