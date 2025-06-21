package com.stripe.onramp.result

/**
 * Result of KYC submission in OnRamp.
 */
sealed class OnRampKycResult {
    /**
     * KYC submission completed successfully.
     */
    object Success : OnRampKycResult()

    /**
     * KYC submission failed due to an error.
     * @param error The error that caused the failure.
     */
    data class Failed(val error: Throwable) : OnRampKycResult()
} 