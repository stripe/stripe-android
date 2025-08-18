package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an OnRamp Link user lookup operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampRegisterUserResult {
    /**
     * User registration was successful.
     * @param customerId The identifier of the crypto customer that was registered.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val customerId: String
    ) : OnrampRegisterUserResult

    /**
     * User registration failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampRegisterUserResult
}
