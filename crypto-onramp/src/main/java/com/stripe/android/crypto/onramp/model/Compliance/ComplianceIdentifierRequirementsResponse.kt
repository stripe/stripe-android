package com.stripe.android.crypto.onramp.model.Compliance

import kotlinx.serialization.Serializable

@Serializable
internal data class ComplianceIdentifierRequirementsResponse(
    val identifiers: List<ComplianceIdentifierRequirementResponse> = emptyList(),
    val alternatives: List<ComplianceIdentifierAlternativeGroupResponse> = emptyList(),
) {
    fun toComplianceIdentifierRequirements(): ComplianceIdentifierRequirements {
        return ComplianceIdentifierRequirements(
            identifiers = identifiers.map { it.toComplianceIdentifierRequirement() },
            alternatives = alternatives.map { it.toComplianceIdentifierAlternativeGroup() }
        )
    }
}
