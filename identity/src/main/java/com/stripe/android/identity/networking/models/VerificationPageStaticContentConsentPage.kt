package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageStaticContentConsentPage(
    @SerialName("accept_button_text")
    val acceptButtonText: String,
    @SerialName("body")
    val body: String,
    @SerialName("decline_button_text")
    val declineButtonText: String,
    @SerialName("scroll_to_continue_button_text")
    val scrollToContinueButtonText: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("privacy_policy")
    val privacyPolicy: String?,
    @SerialName("time_estimate")
    val timeEstimate: String?
)
