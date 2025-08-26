package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampAuthenticationCallback {
    fun onResult(result: OnrampAuthenticationResult)
}

/**
 * Result of an Onramp user authentication operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampAuthenticationResult {
    /**
     * The Link authentication was successful.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val customerId: String
    ) : OnrampAuthenticationResult

    /**
     * The Link authentication was cancelled.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampAuthenticationResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthenticationResult
}
