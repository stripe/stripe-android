package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an Onramp Link user lookup operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampLinkLookupResult {
    /**
     * Link user lookup was successful.
     * @param hasLinkAccount Whether the email is associated with an existing Link consumer, or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val hasLinkAccount: Boolean
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
