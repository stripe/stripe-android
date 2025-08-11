package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an OnRamp Link user lookup operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampLinkLookupResult {
    /**
     * Link user lookup was successful.
     * @param isLinkUser Whether the email belongs to an existing Link user
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val isLinkUser: Boolean
    ) : OnrampLinkLookupResult

    /**
     * Link user lookup failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampLinkLookupResult
}
