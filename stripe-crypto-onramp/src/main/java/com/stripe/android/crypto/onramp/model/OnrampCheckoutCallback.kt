package com.stripe.android.crypto.onramp.model

/**
 * Callback interface for checkout operations.
 */
fun interface OnrampCheckoutCallback {
    /**
     * Called when a checkout operation completes.
     *
     * @param result The result of the checkout operation.
     */
    fun onResult(result: OnrampCheckoutResult)
}
