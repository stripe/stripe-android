package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticConsentLineContent(
    @SerialName("icon")
    val icon: VerificationPageIconType,
    @SerialName("content")
    val content: String
) : Parcelable
