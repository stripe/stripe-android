package com.stripe.android.crypto.onramp.kycverification

import com.stripe.android.crypto.onramp.model.KycRequirements

internal sealed interface KycVerificationStep {
    data object CollectKycInfo : KycVerificationStep

    data object ReviewKycInfo : KycVerificationStep

    data object VerifyIdentity : KycVerificationStep

    data object AcceptTermsOfService : KycVerificationStep

    data object Attest : KycVerificationStep

    data class FulfillKycRequirements(val requirements: KycRequirements) : KycVerificationStep
}

internal fun interface KycVerificationStepExecutor {
    suspend fun execute(step: KycVerificationStep): KycVerificationStepResult
}

internal sealed interface KycVerificationStepResult {
    /** Includes submission when required. Refresh status afterward; completion is not KYC approval. */
    data object Completed : KycVerificationStepResult

    /** User dismissal. Coroutine cancellation propagates instead of returning this result. */
    data object Cancelled : KycVerificationStepResult

    data class Failed(val error: Throwable) : KycVerificationStepResult
}
