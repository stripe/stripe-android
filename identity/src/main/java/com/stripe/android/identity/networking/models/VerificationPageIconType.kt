package com.stripe.android.identity.networking.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.identity.R
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

@DrawableRes
internal fun VerificationPageIconType.getResourceId() =
    when (this) {
        VerificationPageIconType.CLOUD -> R.drawable.stripe_cloud_icon
        VerificationPageIconType.DOCUMENT -> R.drawable.stripe_document_icon
        VerificationPageIconType.CREATE_IDENTITY_VERIFICATION ->
            R.drawable.stripe_create_identity_verification_icon
        VerificationPageIconType.LOCK -> R.drawable.stripe_lock_icon
        VerificationPageIconType.MOVED -> R.drawable.stripe_moved_icon
        VerificationPageIconType.WALLET -> R.drawable.stripe_wallet_icon
        VerificationPageIconType.CAMERA -> R.drawable.stripe_camera_icon
        VerificationPageIconType.DISPUTE_PROTECTION -> R.drawable.stripe_dispute_protection_icon
        VerificationPageIconType.PHONE -> R.drawable.stripe_phone_icon
    }

@StringRes
internal fun VerificationPageIconType.getContentDescriptionId() =
    when (this) {
        VerificationPageIconType.CLOUD -> R.string.stripe_description_camera
        VerificationPageIconType.DOCUMENT -> R.string.stripe_description_document
        VerificationPageIconType.CREATE_IDENTITY_VERIFICATION ->
            R.string.stripe_description_create_identity_verification
        VerificationPageIconType.LOCK -> R.string.stripe_description_lock
        VerificationPageIconType.MOVED -> R.string.stripe_description_moved
        VerificationPageIconType.WALLET -> R.string.stripe_description_wallet
        VerificationPageIconType.CAMERA -> R.string.stripe_description_camera
        VerificationPageIconType.DISPUTE_PROTECTION -> R.string.stripe_description_dispute_protection
        VerificationPageIconType.PHONE -> R.string.stripe_description_phone
    }
