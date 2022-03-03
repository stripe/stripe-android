package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageStaticContentDocumentSelectPage(

    @SerialName("button_text")
    val buttonText: String,
    @SerialName("id_document_type_allowlist")
    val idDocumentTypeAllowlist: Map<String, String>,
    @SerialName("title")
    val title: String
)
