package com.stripe.android.crypto.onramp.model.compliance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ComplianceIdentifierRequirementsResponse(
    val identifiers: List<ComplianceIdentifierRequirementResponse> = emptyList(),
    val alternatives: List<ComplianceIdentifierAlternativeGroupResponse> = emptyList(),
    @SerialName("carf_tin_required")
    val carfTinRequired: Boolean = false,
) {
    fun toComplianceIdentifierRequirements(): ComplianceIdentifierRequirements {
        return ComplianceIdentifierRequirements(
            identifiers = identifiers.map { it.toComplianceIdentifierRequirement() },
            alternatives = alternatives.map { it.toComplianceIdentifierAlternativeGroup() },
            carfTinRequired = carfTinRequired
        )
    }
}
