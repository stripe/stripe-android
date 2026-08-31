package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal sealed interface PartnerTerms {
    data class Required(
        val partner: String,
        val version: String,
        val text: String,
    ) : PartnerTerms

    data object NotRequired : PartnerTerms
}

@Serializable
internal data class PartnerTermsResponse(
    val required: Boolean,
    val partner: String? = null,
    val version: String? = null,
    @SerialName("text")
    val text: String? = null,
) {
    fun toPartnerTerms(): PartnerTerms {
        if (!required) {
            return PartnerTerms.NotRequired
        }

        return PartnerTerms.Required(
            partner = requireNotNull(partner),
            version = requireNotNull(version),
            text = requireNotNull(text),
        )
    }
}

@Serializable
internal data class ConfirmPartnerTermsRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    val version: String,
)
