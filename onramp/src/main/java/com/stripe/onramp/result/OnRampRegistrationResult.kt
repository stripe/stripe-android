package com.stripe.onramp.result

/**
 * Result of Link user registration process in OnRamp.
 */
sealed class OnRampRegistrationResult {
    /**
     * Registration completed successfully.
     * @param customerId The ID of the newly registered crypto customer.
     */
    data class Success(val customerId: String) : OnRampRegistrationResult()

    /**
     * Registration failed due to an error.
     * @param error The error that caused the failure.
     */
    data class Failed(val error: Throwable) : OnRampRegistrationResult()
} 