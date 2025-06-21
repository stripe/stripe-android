package com.stripe.onramp.result

/**
 * Result of OnRamp configuration process.
 */
sealed class OnRampConfigureResult {
    /**
     * Configuration completed successfully.
     */
    object Success : OnRampConfigureResult()

    /**
     * Configuration failed due to an error.
     * @param error The error that caused the failure.
     */
    data class Failed(val error: Throwable) : OnRampConfigureResult()
} 