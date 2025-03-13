package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PublishableKeyRequest(
    @SerialName("account")
    val country: MerchantCountry,
) {
    @Serializable
    data class Response(
        @SerialName("publishable_key")
        val publishableKey: String
    )
}
