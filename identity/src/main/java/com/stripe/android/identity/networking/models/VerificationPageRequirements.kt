package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageRequirements(
    @SerialName("missing")
    val missing: List<Requirement>
)
