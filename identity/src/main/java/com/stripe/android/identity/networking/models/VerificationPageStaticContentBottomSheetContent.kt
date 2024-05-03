package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentBottomSheetContent(
    @SerialName("bottomsheet_id")
    val bottomSheetId: String,
    @SerialName("title")
    val title: String?,
    @SerialName("lines")
    val lines: List<VerificationPageStaticContentBottomSheetLineContent>
) : Parcelable
