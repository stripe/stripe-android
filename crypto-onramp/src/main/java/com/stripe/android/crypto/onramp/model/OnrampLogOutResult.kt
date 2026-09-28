package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of logging out from Link.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampLogOutResult {
    /**
     * The user successfully logged out from Link.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampLogOutResult

    /**
     * An error occurred while logging out from Link.
     * @param error The error that occurred during logout.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampLogOutResult
}
