package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CreateLinkAccountSessionResponse(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("publishable_key") val publishableKey: String
)
