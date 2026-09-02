package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal sealed interface PartnerTerms {
    data class Required(
        val partner: String,
        val declarationId: String,
        val text: String,
    ) : PartnerTerms

    data object NotRequired : PartnerTerms
}

@Serializable
internal enum class CryptoOnrampPartner {
    @SerialName("swapped")
    Swapped,
}

@Serializable
internal enum class PartnerDeclarationType {
    @SerialName("terms")
    TermsAndConditions,

    @SerialName("tos")
    TermsOfService,
}

@Serializable
internal data class RetrievePartnerTermsRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    val partner: CryptoOnrampPartner,
    @SerialName("declaration_type")
    val declarationType: PartnerDeclarationType,
)

@Serializable
internal data class PartnerTermsResponse(
    val required: Boolean,
    val partner: String? = null,
    @SerialName("declaration_id")
    val declarationId: String? = null,
    @SerialName("text")
    val text: String? = null,
) {
    fun toPartnerTerms(): PartnerTerms {
        if (!required) {
            return PartnerTerms.NotRequired
        }

        return PartnerTerms.Required(
            partner = requireNotNull(partner),
            declarationId = requireNotNull(declarationId),
            text = requireNotNull(text),
        )
    }
}

@Serializable
internal data class ConfirmPartnerTermsRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    @SerialName("declaration_id")
    val declarationId: String,
)
