package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed interface OnrampStartVerificationResult {
    /**
     * Starting verification completed successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val response: StartIdentityVerificationResponse
    ) : OnrampStartVerificationResult

    /**
     * Starting verification failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartVerificationResult
}
