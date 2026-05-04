package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of submitting compliance identifiers.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampSubmitIdentifiersResult {
    /**
     * Identifier submission completed and returned a validation result.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val result: SubmitIdentifiersResult
    ) : OnrampSubmitIdentifiersResult

    /**
     * Identifier submission failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampSubmitIdentifiersResult
}
