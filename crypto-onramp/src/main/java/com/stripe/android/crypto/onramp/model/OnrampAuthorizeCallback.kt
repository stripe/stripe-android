package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampAuthorizeCallback {
    fun onResult(result: OnrampAuthorizeResult)
}

/**
 * Result of an OnRamp authorize operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampAuthorizeResult {
    /**
     * The user granted consent to the scopes requested by the LinkAuthIntent.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Consented(
        val customerId: String
    ) : OnrampAuthorizeResult

    /**
     * The user denied consent to the scopes requested by the LinkAuthIntent.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Denied internal constructor() : OnrampAuthorizeResult

    /**
     * The user canceled the authorization.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Canceled internal constructor() : OnrampAuthorizeResult

    /**
     * Authorization failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthorizeResult
}
