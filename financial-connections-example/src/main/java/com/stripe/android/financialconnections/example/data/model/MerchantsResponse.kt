package com.stripe.android.financialconnections.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchantsResponse(
    val merchants: List<Merchant>
)

@Serializable
data class Merchant(
    val name: String,
    val value: String,
    val canSwitchBetweenTestAndLive: Boolean,
) {

    companion object {

        fun default(): Merchant {
            return Merchant("Default", "default", canSwitchBetweenTestAndLive = true)
        }
    }
}
