package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CreateLinkAccountSessionResponse(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("las_id") val lasId: String,
    @SerialName("publishable_key") val publishableKey: String
)

@Keep
@Serializable
data class CreateIntentResponse(
    @SerialName("secret") val intentSecret: String,
    @SerialName("publishable_key") val publishableKey: String
)
