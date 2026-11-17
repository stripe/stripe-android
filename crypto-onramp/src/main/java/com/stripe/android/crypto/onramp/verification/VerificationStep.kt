package com.stripe.android.crypto.onramp.verification

import com.stripe.android.crypto.onramp.model.KycRequirements

/** Supported verification capabilities; this does not prescribe their order or regional policy. */
internal sealed interface VerificationStep {
    /** Collect, validate, and submit initial customer details through the existing KYC API. */
    data object CollectKycInfo : VerificationStep

    /** Review existing details, including the address-editing round trip, and submit confirmation. */
    data object ReviewKycInfo : VerificationStep

    /** Present the existing Identity verification flow. */
    data object VerifyIdentity : VerificationStep

    /** Present and accept onboarding terms if needed; transaction terms remain purchase-owned. */
    data object AcceptTermsOfService : VerificationStep

    /** Present and submit the existing user attestation. */
    data object Attest : VerificationStep

    /** Collect supported additional documents/questionnaires using the existing KYC flow. */
    data class FulfillKycRequirements(val requirements: KycRequirements) : VerificationStep
}

/**
 * Presents and completes one step for the current customer, including its submission when applicable.
 *
 * The Android host owns presentation and routes results here without also invoking merchant callbacks
 * for the same operation. Existing manually launched operations retain their original callbacks.
 * Implementations must not launch a purchase or decide the next verification step.
 *
 * User dismissal returns [VerificationStepResult.Cancelled]. Coroutine cancellation propagates so
 * destroying a host does not accidentally report a user decision or continue into another screen.
 */
internal fun interface VerificationStepExecutor {
    suspend fun execute(context: VerificationContext, step: VerificationStep): VerificationStepResult
}

internal sealed interface VerificationStepResult {
    /**
     * The step finished (or was already unnecessary), not that the customer is verified.
     * Resolve authoritative status again before continuing. An unchanged action must not cause an
     * unbounded presentation loop; it also does not establish that the backend is reviewing the user.
     */
    data object Completed : VerificationStepResult

    data object Cancelled : VerificationStepResult

    data class Failed(val error: Throwable) : VerificationStepResult
}
