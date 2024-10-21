package com.stripe.android.connect.example.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetAccountsResponse(
    @SerialName("publishable_key")
    val publishableKey: String,
    @SerialName("available_merchants")
    val availableMerchants: List<Merchant>
)
