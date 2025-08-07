package com.stripe.android.crypto.onramp.model

internal sealed interface OnrampStartVerificationResult {
    /**
     * Starting verification completed successfully.
     */
    class Completed internal constructor(
        val response: StartIdentityVerificationResponse
    ) : OnrampStartVerificationResult

    /**
     * Starting verification failed due to an error.
     * @param error The error that caused the failure.
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartVerificationResult
}
