package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentIndividualWelcomePage(
    @SerialName("get_started_button_text")
    val getStartedButtonText: String,
    @SerialName("title")
    val title: String,
    @SerialName("privacy_policy")
    val privacyPolicy: String,
    @SerialName("lines")
    val lines: List<VerificationPageStaticConsentLineContent>
) : Parcelable
