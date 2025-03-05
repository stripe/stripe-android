package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentDocumentSelectPage(

    @SerialName("button_text")
    val buttonText: String,
    @SerialName("id_document_type_allowlist")
    val idDocumentTypeAllowlist: Map<String, String>,
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String?

) : Parcelable