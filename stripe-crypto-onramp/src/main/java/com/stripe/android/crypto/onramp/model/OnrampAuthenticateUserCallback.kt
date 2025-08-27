package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampAuthenticateUserCallback {
    fun onResult(result: OnrampAuthenticateResult)
}

/**
 * Result of an Onramp user authentication operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampAuthenticateResult {
    /**
     * The Link authentication was successful.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val customerId: String
    ) : OnrampAuthenticateResult

    /**
     * The Link authentication was cancelled.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampAuthenticateResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthenticateResult
}
