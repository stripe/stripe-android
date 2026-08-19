package com.stripe.android.crypto.onramp.model

internal data class AdditionalKycRequirements(
    val userActionRequired: List<AdditionalKycRequirementResponse>,
    val pendingPartnerAction: List<AdditionalKycRequirementResponse>,
    val pendingStripeAction: List<AdditionalKycRequirementResponse>,
    val unrecognizedActionOwner: List<AdditionalKycRequirementResponse>,
)
