package com.stripe.android.crypto.onramp.model.compliance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitIdentifiersResponse(
    val completed: Boolean,
    val identifiers: List<ComplianceIdentifierRequirementResponse> = emptyList(),
    val alternatives: List<ComplianceIdentifierAlternativeGroupResponse> = emptyList(),
    @SerialName("invalid_identifiers")
    val invalidIdentifiers: List<String> = emptyList(),
    @SerialName("carf_tin_required")
    val carfTinRequired: Boolean,
) {
    fun toSubmitIdentifiersResult(): SubmitIdentifiersResult {
        return SubmitIdentifiersResult(
            completed = completed,
            identifiers = identifiers.map { it.toComplianceIdentifierRequirement() },
            alternatives = alternatives.map { it.toComplianceIdentifierAlternativeGroup() },
            invalidIdentifiers = invalidIdentifiers.map(ComplianceIdentifierType::fromValue),
            carfTinRequired = carfTinRequired
        )
    }
}
