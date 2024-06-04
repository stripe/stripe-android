package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageDataRequirements(

    @SerialName("errors")
    val errors: List<VerificationPageDataRequirementError>,

    @SerialName("missing")
    val missings: List<Requirement>? = null

)
