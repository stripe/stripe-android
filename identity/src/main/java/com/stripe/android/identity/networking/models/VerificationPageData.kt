package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VerificationPageData contains the state of a verification, including what information needs to be collected to complete the verification flow.
 */
@Serializable
internal data class VerificationPageData(

    /* Unique identifier for the object. */
    @SerialName("id")
    val id: String,
    /* String representing the object's type. Objects of the same type share the same value. */
    @SerialName("object")
    val objectType: String,
    @SerialName("requirements")
    val requirements: VerificationPageDataRequirements,
    /* Status of the associated VerificationSession. */
    @SerialName("status")
    val status: Status,
    /* If true, the associated VerificationSession has been submitted for processing. */
    @SerialName("submitted")
    val submitted: Boolean,
    /* If true, the associated VerificationSession has been is finished and can no longer be changed. */
    @SerialName("closed")
    val closed: Boolean,
) {
    /**
     * Status of the associated VerificationSession.
     * Values: CANCELED,PROCESSING,REQUIRESINPUT,VERIFIED
     */
    @Serializable
    internal enum class Status {
        @SerialName("canceled")
        CANCELED,

        @SerialName("processing")
        PROCESSING,

        @SerialName("requires_input")
        REQUIRESINPUT,

        @SerialName("verified")
        VERIFIED
    }

    internal companion object {
        fun VerificationPageData.hasError() = requirements.errors.isNotEmpty()
        fun VerificationPageData.isMissingConsent() =
            requirements.missings?.contains(Requirement.BIOMETRICCONSENT) == true

        fun VerificationPageData.isMissingFront() =
            requirements.missings?.contains(Requirement.IDDOCUMENTFRONT) == true

        fun VerificationPageData.isMissingBack() =
            requirements.missings?.contains(Requirement.IDDOCUMENTBACK) == true

        fun VerificationPageData.isMissingSelfie() =
            requirements.missings?.contains(Requirement.FACE) == true

        fun VerificationPageData.isMissingOtp() =
            requirements.missings?.contains(Requirement.PHONE_OTP) == true

        fun VerificationPageData.isMissingIndividualRequirements() =
            requirements.missings?.intersect(
                listOf(
                    Requirement.IDNUMBER,
                    Requirement.DOB,
                    Requirement.NAME,
                    Requirement.ADDRESS,
                    Requirement.PHONE_NUMBER
                )
            )?.isNotEmpty() == true

        /**
         * When submitted but is not closed and there is still missing requirements, need to
         * fallback.
         */
        fun VerificationPageData.needsFallback() =
            submitted && !closed && requirements.missings?.isEmpty() == false

        fun VerificationPageData.submittedAndClosed() = submitted && closed
    }
}
