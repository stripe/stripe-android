package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CreateIntentResponse(
    @SerialName("secret") val intentSecret: String,
    @SerialName("publishable_key") val publishableKey: String
)
