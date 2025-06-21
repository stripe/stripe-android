package com.stripe.onramp.result

/**
 * Result of Link verification process in OnRamp.
 */
sealed class OnRampVerificationResult {
    /**
     * Verification completed successfully.
     * @param customerId The ID of the authenticated crypto customer.
     */
    data class Completed(val customerId: String) : OnRampVerificationResult()

    /**
     * Verification was canceled by the user.
     */
    object Canceled : OnRampVerificationResult()

    /**
     * Verification failed due to an error.
     * @param error The error that caused the failure.
     */
    data class Failed(val error: Throwable) : OnRampVerificationResult()
}