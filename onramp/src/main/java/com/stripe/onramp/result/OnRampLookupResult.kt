package com.stripe.onramp.result

/**
 * Result of an OnRamp Link user lookup operation.
 */
sealed class OnRampLookupResult {
    /**
     * Link user lookup was successful.
     * @param email The email that was looked up
     * @param isLinkUser Whether the email belongs to an existing Link user
     */
    data class Success(
        val email: String,
        val isLinkUser: Boolean
    ) : OnRampLookupResult()

    /**
     * Link user lookup failed.
     * @param email The email that was looked up
     * @param error The error that caused the failure
     */
    data class Failed(
        val email: String,
        val error: Throwable
    ) : OnRampLookupResult()
} 