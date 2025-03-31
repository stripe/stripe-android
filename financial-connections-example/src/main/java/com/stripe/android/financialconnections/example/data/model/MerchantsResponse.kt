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

        fun hardcoded(): List<Merchant> {
            return listOf(
                Merchant("Default", "default", canSwitchBetweenTestAndLive = true),
                Merchant("PartnerD", "partner_d", canSwitchBetweenTestAndLive = false),
                Merchant("PartnerF", "partner_f", canSwitchBetweenTestAndLive = false),
                Merchant("PartnerM", "partner_m", canSwitchBetweenTestAndLive = false),
                Merchant("PlatformC", "strash", canSwitchBetweenTestAndLive = true),
                Merchant("Networking", "networking", canSwitchBetweenTestAndLive = true),
                Merchant("LiveTesting", "live_testing", canSwitchBetweenTestAndLive = false),
                Merchant("TestMode", "testmode", canSwitchBetweenTestAndLive = false),
                Merchant("Trusted", "trusted", canSwitchBetweenTestAndLive = false),
                Merchant("Custom", "other", canSwitchBetweenTestAndLive = true),
            )
        }
    }
}
