package com.stripe.android.identity.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerificationSessionCreationResponse(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("ephemeral_key_secret") val ephemeralKeySecret: String,
    @SerialName("id") val verificationSessionId: String,
    @SerialName("url") val url: String
)

@Serializable
data class VerificationSessionCreationRequest(
    @SerialName("options") val options: Options? = null,
    @SerialName("type") val type: String = "document"
) {
    @Serializable
    data class Options(
        @SerialName("document") val document: Document? = null
    )

    @Serializable
    data class Document(
        @SerialName("allowed_types") val allowedTypes: List<String>? = null,
        @SerialName("require_id_number") val requireIdNumber: Boolean? = null,
        @SerialName("require_live_capture") val requireLiveCapture: Boolean? = null,
        @SerialName("require_matching_selfie") val requireMatchingSelfie: Boolean? = null
    )
}
