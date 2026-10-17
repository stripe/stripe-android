package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * The compliance identifiers a customer still needs to provide.
 *
 * @property identifiers The MiCA identifiers that still need to be provided.
 * @property alternatives Alternative MiCA identifiers that can satisfy a requirement.
 * @property carfTinRequired Whether the customer still needs to provide at least one CRS/CARF TIN.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierRequirements internal constructor(
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>,
    val carfTinRequired: Boolean
)
