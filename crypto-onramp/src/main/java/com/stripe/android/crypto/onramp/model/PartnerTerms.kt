package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal sealed interface PartnerTerms {
    data class Required(
        val partner: String,
        val declaration: Declaration,
    ) : PartnerTerms

    data object NotRequired : PartnerTerms

    @Serializable
    data class Declaration(
        val id: String,
        val type: PartnerDeclarationType,
        @SerialName("text")
        val text: String,
    )
}

@Serializable
internal enum class PartnerDeclarationType {
    @SerialName("transaction_terms")
    TransactionTerms,

    @SerialName("terms_of_service")
    TermsOfService,
}

@Serializable
internal data class RetrievePartnerTermsRequest(
    @SerialName("declaration_type")
    val declarationType: PartnerDeclarationType,
)

@Serializable
internal data class PartnerTermsResponse(
    val required: Boolean,
    val partner: String? = null,
    val declaration: PartnerTerms.Declaration? = null,
) {
    fun toPartnerTerms(): PartnerTerms {
        if (!required) {
            return PartnerTerms.NotRequired
        }

        return PartnerTerms.Required(
            partner = requireNotNull(partner),
            declaration = requireNotNull(declaration),
        )
    }
}

@Serializable
internal data class ConfirmPartnerTermsRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    @SerialName("declaration_id")
    val declarationId: String,
)
