package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Callback interface for checkout operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampCheckoutCallback {
    /**
     * Called when a checkout operation completes.
     *
     * @param result The result of the checkout operation.
     */
    fun onResult(result: OnrampCheckoutResult)
}
