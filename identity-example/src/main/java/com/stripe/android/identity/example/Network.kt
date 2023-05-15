package com.stripe.android.identity.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationSessionCreationResponse(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("ephemeral_key_secret") val ephemeralKeySecret: String,
    @SerialName("id") val verificationSessionId: String,
    @SerialName("url") val url: String
)

@Serializable
internal data class VerificationSessionCreationRequest(
    @SerialName("type") val type: String = "document",
    @SerialName("options") val options: Options? = null,
    @SerialName("provided_details") val providedDetails: ProvidedDetails? = null
) {
    @Serializable
    data class Options(
        @SerialName("document") val document: Document? = null,
        @SerialName("phone") val phone: Phone? = null,
        @SerialName("phone_otp") val phoneOtp: PhoneOTP? = null,
        @SerialName("phone_records") val phoneRecords: PhoneRecords? = null
    ) {
        @Serializable
        data class Phone(
            @SerialName("require_verification") val requireVerification: Boolean? = null
        )

        @Serializable
        data class Document(
            @SerialName("allowed_types") val allowedTypes: List<DocumentType>? = null,
            @SerialName("require_id_number") val requireIdNumber: Boolean? = null,
            @SerialName("require_live_capture") val requireLiveCapture: Boolean? = null,
            @SerialName("require_matching_selfie") val requireMatchingSelfie: Boolean? = null,
            @SerialName("require_address") val requireAddress: Boolean? = null
        )

        @Serializable
        data class PhoneOTP(
            @SerialName("check") val check: PhoneOTPCheck? = null,
        )

        @Serializable
        data class PhoneRecords(
            @SerialName("fallback") val fallback: Fallback? = null,
        )
    }

    @Serializable
    data class ProvidedDetails(
        @SerialName("phone") val phone: String? = null
    )
}

internal enum class DocumentType {
    @SerialName("driving_license")
    DrivingLicense,

    @SerialName("passport")
    Passport,

    @SerialName("id_card")
    IdCard
}

internal enum class Fallback {
    @SerialName("document")
    Document
}

internal enum class PhoneOTPCheck(val value: String) {
    @SerialName("none")
    None("none"),

    @SerialName("attempt")
    Attempt("attempt"),

    @SerialName("required")
    Required("required")
}
