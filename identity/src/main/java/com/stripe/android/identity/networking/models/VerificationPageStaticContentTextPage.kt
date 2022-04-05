package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageStaticContentTextPage(

    @SerialName("body")
    val body: String,
    @SerialName("button_text")
    val buttonText: String,
    @SerialName("title")
    val title: String
)
