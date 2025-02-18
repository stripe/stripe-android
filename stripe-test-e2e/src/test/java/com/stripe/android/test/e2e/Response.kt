package com.stripe.android.test.e2e

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed class Response {
    @JsonClass(generateAdapter = true)
    data class CreatedCardPaymentIntent(
        @field:Json(name = "publishableKey") val publishableKey: String,
        @field:Json(name = "paymentIntent") val clientSecret: String,
        @field:Json(name = "expectedAmount") val amount: Int,
        @field:Json(name = "expectedCurrency") val currency: String,
        @field:Json(name = "expectedAccountID") val accountId: String
    ) : Response()

    @JsonClass(generateAdapter = true)
    data class FetchedCardPaymentIntent(
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
    ) : Response()

    @JsonClass(generateAdapter = true)
    data class CreatedPaymentIntent(
        @field:Json(name = "intent") val intent: String,
        @field:Json(name = "secret") val secret: String,
        @field:Json(name = "status") val status: String
    ) : Response()

    @JsonClass(generateAdapter = true)
    data class CreatedSetupIntent(
        @field:Json(name = "intent") val intent: String,
        @field:Json(name = "secret") val secret: String,
        @field:Json(name = "status") val status: String
    ) : Response()

    @JsonClass(generateAdapter = true)
    data class CreatedEphemeralKey(
        @field:Json(name = "customer") val customerId: String,
        @field:Json(name = "ephemeral_key_secret") val ephemeralKeySecret: String,
    )
}
