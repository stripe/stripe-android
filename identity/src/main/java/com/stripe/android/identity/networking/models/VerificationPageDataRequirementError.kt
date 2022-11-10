package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageDataRequirementError(
    @SerialName("body")
    val body: String? = null,
    @SerialName("back_button_text")
    val backButtonText: String? = null,
    @SerialName("continue_button_text")
    val continueButtonText: String? = null,
    @SerialName("requirement")
    val requirement: Requirement,
    @SerialName("title")
    val title: String? = null
)
