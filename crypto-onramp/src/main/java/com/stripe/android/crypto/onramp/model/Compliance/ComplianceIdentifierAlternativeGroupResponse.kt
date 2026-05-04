package com.stripe.android.crypto.onramp.model.Compliance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ComplianceIdentifierAlternativeGroupResponse(
    @SerialName("original_missing_identifiers")
    val originalMissingIdentifiers: List<String>,
    @SerialName("alternative_missing_identifiers")
    val alternativeMissingIdentifiers: List<String>
) {
    fun toComplianceIdentifierAlternativeGroup(): ComplianceIdentifierAlternativeGroup {
        return ComplianceIdentifierAlternativeGroup(
            originalMissingIdentifiers = originalMissingIdentifiers.map(ComplianceIdentifierType::fromValue),
            alternativeMissingIdentifiers = alternativeMissingIdentifiers.map(ComplianceIdentifierType::fromValue)
        )
    }
}
