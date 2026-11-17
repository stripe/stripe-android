package com.stripe.android.crypto.onramp.verification

/**
 * Outcome of the complete customer verification flow, not an individual screen or purchase.
 *
 * Reopening starts with fresh status for the authenticated customer. The host must not automatically
 * retry a purchase after any outcome. Operational failures are distinct from a backend verification
 * block, and submission completion alone cannot produce [Ready].
 */
internal sealed interface VerificationResult {
    /** Matches the readiness guarantee of [VerificationStatus.Ready]. */
    data object Ready : VerificationResult

    /** Review is outstanding; return control instead of holding the flow open indefinitely. */
    data object Pending : VerificationResult

    /** The customer dismissed the flow. This says nothing about their backend verification status. */
    data object Cancelled : VerificationResult

    /** Matches [VerificationStatus.Blocked]; the reason code is diagnostic, not display copy. */
    data class Blocked(val reasonCode: String) : VerificationResult

    /** An operational failure, including unsupported requirements or an unavailable status source. */
    data class Failed(val error: Throwable) : VerificationResult
}
