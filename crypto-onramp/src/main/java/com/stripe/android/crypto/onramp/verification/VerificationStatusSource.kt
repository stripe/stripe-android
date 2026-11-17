package com.stripe.android.crypto.onramp.verification

/**
 * Resolves the next customer verification action from fresh, authoritative backend state.
 *
 * The source owns requirement interpretation and ordering; the coordinator executes one returned
 * step and resolves again. Unknown or unsupported requirements and unavailable status are failures,
 * never [VerificationStatus.Ready] or [VerificationStatus.Blocked].
 *
 * Implementations must not infer readiness from an empty additional-KYC requirements response or
 * from completion of a collection/Identity screen. The backend readiness contract must be established
 * before implementing that mapping. Coroutine cancellation propagates rather than becoming a failure.
 */
internal fun interface VerificationStatusSource {
    suspend fun resolve(context: VerificationContext): Result<VerificationStatus>
}

internal sealed interface VerificationStatus {
    /** The next supported action, selected by the status source rather than by the merchant. */
    data class RequiresAction(val step: VerificationStep) : VerificationStatus

    /**
     * Authoritative status confirms the customer's current verification requirements are satisfied.
     * This is not payment authorization or a guarantee that a later purchase needs no further KYC.
     */
    data object Ready : VerificationStatus

    /** Review is outstanding; there is no supported customer action to present now. */
    data object Pending : VerificationStatus

    /**
     * The backend explicitly reports that verification cannot proceed through this flow.
     * [reasonCode] is a backend-defined diagnostic code, not text to display to the customer.
     */
    data class Blocked(val reasonCode: String) : VerificationStatus
}
