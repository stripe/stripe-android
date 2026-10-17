package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierRequirements

/**
 * Result of retrieving missing compliance identifiers.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampRetrieveMissingIdentifiersResult {
    /**
     * Missing compliance identifiers were retrieved.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val requirements: ComplianceIdentifierRequirements
    ) : OnrampRetrieveMissingIdentifiersResult

    /**
     * Missing compliance identifier retrieval failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampRetrieveMissingIdentifiersResult
}
