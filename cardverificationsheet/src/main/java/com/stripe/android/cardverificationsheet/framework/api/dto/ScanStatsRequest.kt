package com.stripe.android.cardverificationsheet.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanStatsRequest(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("stats") val stats: String,
)
