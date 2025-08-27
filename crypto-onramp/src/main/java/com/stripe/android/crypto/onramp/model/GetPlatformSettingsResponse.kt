package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetPlatformSettingsResponse(
    @SerialName("publishable_key")
    val publishableKey: String,
)
