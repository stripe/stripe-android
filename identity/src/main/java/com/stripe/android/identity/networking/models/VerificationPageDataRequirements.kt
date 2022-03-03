package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageDataRequirements(

    @SerialName("errors")
    val errors: List<VerificationPageDataRequirementError>,

    @SerialName("missing")
    val missing: List<Missing>
) {
    @Serializable
    internal enum class Missing {
        @SerialName("biometric_consent")
        BIOMETRICCONSENT,

        @SerialName("id_document_back")
        IDDOCUMENTBACK,

        @SerialName("id_document_front")
        IDDOCUMENTFRONT,

        @SerialName("id_document_type")
        IDDOCUMENTTYPE
    }
}
