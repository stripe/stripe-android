package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A VerificationPage contains the static content and initial state that is required for Stripe Identity's native mobile SDKs to render the verification flow.
 */
@Serializable
internal data class VerificationPage(
    @SerialName("biometric_consent")
    val biometricConsent: VerificationPageStaticContentConsentPage,
    @SerialName("document_capture")
    val documentCapture: VerificationPageStaticContentDocumentCapturePage,
    @SerialName("document_select")
    val documentSelect: VerificationPageStaticContentDocumentSelectPage,
    /* The short-lived URL that can be used in the case that the client cannot support the VerificationSession. */
    @SerialName("fallback_url")
    val fallbackUrl: String,
    /* Unique identifier for the object. */
    @SerialName("id")
    val id: String,
    /* Has the value `true` if the object exists in live mode or the value `false` if the object exists in test mode. */
    @SerialName("livemode")
    val livemode: Boolean,
    /* String representing the object's type. Objects of the same type share the same value. */
    @SerialName("object")
    val objectType: String,
    @SerialName("requirements")
    val requirements: VerificationPageRequirements,
    /* Status of the associated VerificationSession. */
    @SerialName("status")
    val status: Status,
    /* If true, the associated VerificationSession has been submitted for processing. */
    @SerialName("submitted")
    val submitted: Boolean,
    @SerialName("success")
    val success: VerificationPageStaticContentTextPage,
    /* If true, the client cannot support the VerificationSession. */
    @SerialName("unsupported_client")
    val unsupportedClient: Boolean,
    @SerialName("welcome")
    val welcome: VerificationPageStaticContentTextPage? = null
) {
    @Serializable
    internal enum class Status {
        @SerialName("canceled")
        CANCELLED,

        @SerialName("processing")
        PROCESSING,

        @SerialName("requires_input")
        REQUIRESINPUT,

        @SerialName("verified")
        VERIFIED;
    }
}
