package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CreateLinkAccountSessionResponse(
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("las_id") val lasId: String,
    @SerializedName("publishable_key") val publishableKey: String
)

@Keep
data class CreateIntentResponse(
    @SerializedName("secret") val intentSecret: String,
    @SerializedName("publishable_key") val publishableKey: String
)
