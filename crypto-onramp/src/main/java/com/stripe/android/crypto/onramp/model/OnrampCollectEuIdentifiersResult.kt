package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of submitting EU identifiers.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampCollectEuIdentifiersResult {
    /**
     * EU identifier submission completed and returned a validation result.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val result: SubmitEuIdentifiersResult
    ) : OnrampCollectEuIdentifiersResult

    /**
     * EU identifier submission failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCollectEuIdentifiersResult
}
