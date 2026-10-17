package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateCardPresentSetupIntentRequest(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
    @SerialName("payment_method_types")
    val paymentMethodTypes: List<String>,
    @SerialName("custom_stripe_api")
    val customStripeApi: String?,
    @SerialName("custom_secret_key")
    val customSecretKey: String?,
    @SerialName("custom_publishable_key")
    val customPublishableKey: String?,
)

@Serializable
data class CreateCardPresentSetupIntentResponse(
    @SerialName("client_secret")
    val clientSecret: String,
)
