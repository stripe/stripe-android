package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CryptoCustomerRequestParams(
    val credentials: Credentials
) {
    @Serializable
    internal data class Credentials(
        @SerialName("consumer_session_client_secret")
        val consumerSessionClientSecret: String
    )
}
