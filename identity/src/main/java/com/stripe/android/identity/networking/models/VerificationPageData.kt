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
    val submitted: Boolean
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
        fun VerificationPageData.isMissingDocumentType() =
            requirements.missing.contains(VerificationPageDataRequirements.Missing.IDDOCUMENTTYPE)

        fun VerificationPageData.isMissingBackOrFront() =
            requirements.missing.contains(VerificationPageDataRequirements.Missing.IDDOCUMENTFRONT) ||
                requirements.missing.contains(VerificationPageDataRequirements.Missing.IDDOCUMENTBACK)

        fun VerificationPageData.hasError() = requirements.errors.isNotEmpty()
    }
}
