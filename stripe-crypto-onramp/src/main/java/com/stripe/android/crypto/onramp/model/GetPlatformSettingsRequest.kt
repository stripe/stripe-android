package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request parameters for getting platform settings.
 */
@Serializable
internal data class GetPlatformSettingsRequest(
    @SerialName("credentials")
    val credentials: Credentials? = null,
    @SerialName("country_hint")
    val countryHint: String? = null
) {
    @Serializable
    internal data class Credentials(
        @SerialName("consumer_session_client_secret")
        val consumerSessionClientSecret: String
    )
}
