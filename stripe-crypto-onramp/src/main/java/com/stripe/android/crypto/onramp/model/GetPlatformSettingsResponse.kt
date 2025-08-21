package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class GetPlatformSettingsResponse(
    @SerialName("publishable_key")
    val publishableKey: String,
)
