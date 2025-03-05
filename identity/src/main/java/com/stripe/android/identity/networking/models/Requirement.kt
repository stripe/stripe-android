package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class Requirement {
    @SerialName("biometric_consent")
    BIOMETRICCONSENT,

    @SerialName("id_document_back")
    IDDOCUMENTBACK,

    @SerialName("id_document_front")
    IDDOCUMENTFRONT,

    @SerialName("face")
    FACE,

    @SerialName("id_number")
    IDNUMBER,

    @SerialName("dob")
    DOB,

    @SerialName("name")
    NAME,

    @SerialName("address")
    ADDRESS,

    @SerialName("phone_number")
    PHONE_NUMBER,

    @SerialName("phone_otp")
    PHONE_OTP;

    internal companion object {
        private val REQUIREMENTS_SUPPORTS_FORCE_CONFIRM = setOf(
            IDDOCUMENTFRONT,
            IDDOCUMENTBACK
        )

        fun Requirement.supportsForceConfirm() =
            REQUIREMENTS_SUPPORTS_FORCE_CONFIRM.contains(this)
    }
}
