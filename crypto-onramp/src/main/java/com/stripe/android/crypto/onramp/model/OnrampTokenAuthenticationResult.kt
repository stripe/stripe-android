package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an OnRamp seamless sign in authentication operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampTokenAuthenticationResult {
    /**
     * Authentication completed successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampTokenAuthenticationResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampTokenAuthenticationResult
}
