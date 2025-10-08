package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an OnRamp seamless sign in authentication operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampAuthenticationResult {
    /**
     * Authentication completed successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Completed : OnrampAuthenticationResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthenticationResult
}
