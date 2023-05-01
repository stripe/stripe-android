package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName

internal enum class VerificationReportRequirement(val value: String) {
    @SerialName("generic")
    GENERIC("generic"),

    @SerialName("address")
    ADDRESS("address"),

    @SerialName("dob")
    DOB("dob"),

    @SerialName("document")
    DOCUMENT("document"),

    @SerialName("email")
    EMAIL("email"),

    @SerialName("id_number")
    ID_NUMBER("id_number"),

    @SerialName("phone")
    PHONE("phone"),

    @SerialName("phone_otp")
    PHONE_OTP("phone_otp"),

    @SerialName("phone_records")
    PHONE_RECORDS("phone_records"),

    @SerialName("selfie")
    SELFIE("selfie")
}
