package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of logging out from Link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampLogOutResult {
    /**
     * The user successfully logged out from Link.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Completed : OnrampLogOutResult

    /**
     * An error occurred while logging out from Link.
     * @param error The error that occurred during logout.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampLogOutResult
}