package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback interface for checkout operations.
 */
@ExperimentalCryptoOnramp
fun interface OnrampCheckoutCallback {
    /**
     * Called when a checkout operation completes.
     *
     * @param result The result of the checkout operation.
     */
    fun onResult(result: OnrampCheckoutResult)
}
