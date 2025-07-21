package com.stripe.android.crypto.onramp.model

fun interface OnrampVerificationCallback {
    fun onResult(result: OnrampVerificationResult)
}

/**
 * Result of an OnRamp User Authentication operation.
 */
sealed interface OnrampVerificationResult {
    /**
     * The link authentication was successful.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    class Completed internal constructor(
        val customerId: String
    ) : OnrampVerificationResult

    /**
     * The link authentication was cancelled.
     */
    class Cancelled internal constructor() : OnrampVerificationResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampVerificationResult
}
