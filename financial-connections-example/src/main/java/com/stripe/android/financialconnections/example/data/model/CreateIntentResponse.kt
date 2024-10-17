package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CreateIntentResponse(
    @SerialName("id") val intentId: String,
    @SerialName("secret") val intentSecret: String,
    @SerialName("publishable_key") val publishableKey: String,
    @SerialName("ephemeral_key") val ephemeralKey: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("amount") val amount: Long,
    @SerialName("currency") val currency: String,
)
