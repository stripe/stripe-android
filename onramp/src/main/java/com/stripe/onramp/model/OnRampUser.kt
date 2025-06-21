package com.stripe.onramp.model

import android.os.Parcelable
import com.stripe.onramp.OnRampCoordinator
import kotlinx.parcelize.Parcelize

/**
 * Represents a crypto customer after successful authentication.
 */
@Parcelize
data class OnRampUser(
    val id: String,
    val kycStatus: KycStatus,
    val documentUploadStatus: DocumentUploadStatus,
    val allowedToAttemptIdentityVerification: Boolean
) : Parcelable


/**
 * KYC verification status.
 */
enum class KycStatus {
    NOT_SUBMITTED,
    PENDING,
    REJECTED,
    APPROVED
}

/**
 * Document upload status.
 */
enum class DocumentUploadStatus {
    NOT_SUBMITTED,
    PENDING,
    REJECTED,
    APPROVED
}