package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentBottomSheetLineContent(
    @SerialName("icon")
    val icon: VerificationPageIconType? = null,
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String
) : Parcelable
