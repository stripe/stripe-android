package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of retrieving identifier requirements.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampGetIdentifierRequirementsResult {
    /**
     * Identifier requirements were retrieved.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val requirements: IdentifierRequirements
    ) : OnrampGetIdentifierRequirementsResult

    /**
     * Identifier requirement retrieval failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampGetIdentifierRequirementsResult
}
