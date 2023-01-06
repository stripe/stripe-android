package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentTextPage(

    @SerialName("body")
    val body: String,
    @SerialName("button_text")
    val buttonText: String,
    @SerialName("title")
    val title: String
) : Parcelable
