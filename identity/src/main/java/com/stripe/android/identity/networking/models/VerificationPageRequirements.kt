package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageRequirements(
    @SerialName("missing")
    val missing: List<Missing>
) {
    @Serializable
    internal enum class Missing {
        @SerialName("biometric_consent")
        BIOMETRICCONSENT,

        @SerialName("id_document_front")
        IDDOCUMENTBACK,

        @SerialName("id_document_back")
        IDDOCUMENTFRONT,

        @SerialName("id_document_type")
        IDDOCUMENTTYPE,

        // TODO(IDPROD-3944) - verify with server change
        @SerialName("face")
        FACE,

        @SerialName("training_consent")
        TRAININGCONSENT
    }
}
