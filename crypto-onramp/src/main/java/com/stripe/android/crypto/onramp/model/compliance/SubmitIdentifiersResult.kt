package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * The result of submitting compliance identifiers for MiCA and CRS/CARF compliance.
 */
@ExperimentalCryptoOnramp
@Poko
class SubmitIdentifiersResult internal constructor(
    val valid: Boolean,
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>,
    val invalidIdentifiers: List<ComplianceIdentifierType>
)
