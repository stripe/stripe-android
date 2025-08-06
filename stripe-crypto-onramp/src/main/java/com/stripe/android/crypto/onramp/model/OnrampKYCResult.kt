package com.stripe.android.crypto.onramp.model

/**
 * Result of KYC submission in Onramp.
 */
sealed interface OnrampKYCResult {
    /**
     * KYC submission completed successfully.
     */
    data object Completed : OnrampKYCResult

    /**
     * KYC submission failed due to an error.
     * @param error The error that caused the failure.
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampKYCResult
}
