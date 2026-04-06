package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of a checkout operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampCheckoutResult {
    /**
     * Checkout completed successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampCheckoutResult

    /**
     * Checkout was canceled by the user.
     */
    @ExperimentalCryptoOnramp
    class Canceled internal constructor() : OnrampCheckoutResult

    /**
     * Checkout failed with an error.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(val error: Throwable) : OnrampCheckoutResult
}
