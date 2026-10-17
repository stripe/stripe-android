package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an Onramp Link user lookup operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampHasLinkAccountResult {
    /**
     * Link user lookup was successful.
     * @param hasLinkAccount Whether the email is associated with an existing Link consumer, or `false` otherwise.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val hasLinkAccount: Boolean
    ) : OnrampHasLinkAccountResult

    /**
     * Link user lookup failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampHasLinkAccountResult
}
