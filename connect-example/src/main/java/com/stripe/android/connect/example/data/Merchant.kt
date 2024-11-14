package com.stripe.android.connect.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Merchant(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("display_name")
    val displayName: String
)
