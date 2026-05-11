package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * The compliance identifiers a customer still needs to provide.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierRequirements internal constructor(
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>
)
