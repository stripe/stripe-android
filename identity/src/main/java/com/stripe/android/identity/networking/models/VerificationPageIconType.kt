package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class VerificationPageIconType {
    @SerialName("cloud")
    CLOUD,

    @SerialName("document")
    DOCUMENT,

    @SerialName("create_identity_verification")
    CREATE_IDENTITY_VERIFICATION,

    @SerialName("lock")
    LOCK,

    @SerialName("moved")
    MOVED,

    @SerialName("wallet")
    WALLET,

    @SerialName("camera")
    CAMERA,

    @SerialName("dispute_protection")
    DISPUTE_PROTECTION,

    @SerialName("phone")
    PHONE
}

