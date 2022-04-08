package com.stripe.android.connections.example.data.model

import com.google.gson.annotations.SerializedName

data class CreateLinkAccountSessionResponse(
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("las_id") val lasId: String,
    @SerializedName("publishable_key") val publishableKey: String
)
