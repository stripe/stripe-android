package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of a checkout operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampCheckoutResult {
    /**
     * Checkout completed successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor() : OnrampCheckoutResult

    /**
     * Checkout was canceled by the user.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Canceled internal constructor() : OnrampCheckoutResult

    /**
     * Checkout failed with an error.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(val error: Throwable) : OnrampCheckoutResult
}
