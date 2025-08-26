package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampVerifyIdentityCallback {
    fun onResult(result: OnrampVerifyIdentityResult)
}

/**
 * Result of Stripe Identity verification process in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class OnrampVerifyIdentityResult {
    /**
     * The user has completed uploading their documents.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor() : OnrampVerifyIdentityResult()

    /**
     * The user did not complete uploading their document, and should be allowed to try again.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampVerifyIdentityResult()

    /**
     * Identity verification failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(val error: Throwable) : OnrampVerifyIdentityResult()
}
