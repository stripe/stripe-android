package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams
import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitIdentifiersRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    val identifiers: List<ComplianceIdentifierRequest>
)

@Serializable
internal data class ComplianceIdentifierRequest(
    val type: String,
    val value: String
)

internal fun List<ComplianceIdentifier>.toRequest(
    credentials: CryptoCustomerRequestParams.Credentials
): SubmitIdentifiersRequest {
    return SubmitIdentifiersRequest(
        credentials = credentials,
        identifiers = map { it.build().toRequest() }
    )
}

private fun ComplianceIdentifier.State.toRequest(): ComplianceIdentifierRequest {
    return ComplianceIdentifierRequest(
        type = type.value,
        value = value
    )
}
