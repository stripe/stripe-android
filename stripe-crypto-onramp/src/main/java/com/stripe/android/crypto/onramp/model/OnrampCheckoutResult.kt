package com.stripe.android.crypto.onramp.model

/**
 * Result of a checkout operation.
 */
sealed interface OnrampCheckoutResult {
    /**
     * Checkout completed successfully.
     */
    data object Completed : OnrampCheckoutResult

    /**
     * Checkout was canceled by the user.
     */
    data object Canceled : OnrampCheckoutResult

    /**
     * Checkout failed with an error.
     */
    data class Failed(val error: Throwable) : OnrampCheckoutResult
}
