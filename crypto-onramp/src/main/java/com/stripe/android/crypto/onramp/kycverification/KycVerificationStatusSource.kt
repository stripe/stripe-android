package com.stripe.android.crypto.onramp.kycverification

internal fun interface KycVerificationStatusSource {
    suspend fun resolve(): Result<KycVerificationStatus>
}

internal sealed interface KycVerificationStatus {
    data class RequiresAction(val step: KycVerificationStep) : KycVerificationStatus

    /** Backend-confirmed KYC readiness; a later purchase may require further verification. */
    data object Ready : KycVerificationStatus

    data object Pending : KycVerificationStatus

    data object Blocked : KycVerificationStatus
}
