package com.stripe.android.crypto.onramp.model.compliance

import kotlinx.serialization.Serializable

@Serializable
internal data class ComplianceIdentifierRequirementResponse(
    val type: String,
    val regulation: String
) {
    fun toComplianceIdentifierRequirement(): ComplianceIdentifierRequirement {
        return ComplianceIdentifierRequirement(
            type = ComplianceIdentifierType.fromValue(type),
            regulation = requireNotNull(ComplianceRegulation.fromValue(regulation)) {
                "Unrecognized compliance regulation: $regulation"
            }
        )
    }
}
