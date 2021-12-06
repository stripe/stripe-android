package com.stripe.android.stripecardscan.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanStatsRequest(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("payload") val payload: StatsPayload,
)
