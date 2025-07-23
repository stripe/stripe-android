package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class CryptoCustomerRequestParams(
    val credentials: Credentials
) {
    @Serializable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal data class Credentials(
        @SerialName("consumer_session_client_secret")
        val consumerSessionClientSecret: String
    )
}
