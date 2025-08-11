package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampVerificationCallback {
    fun onResult(result: OnrampVerificationResult)
}

/**
 * Result of an OnRamp User Authentication operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampVerificationResult {
    /**
     * The link authentication was successful.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val customerId: String
    ) : OnrampVerificationResult

    /**
     * The link authentication was cancelled.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampVerificationResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampVerificationResult
}
