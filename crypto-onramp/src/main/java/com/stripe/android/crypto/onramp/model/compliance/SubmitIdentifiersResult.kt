package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * The result of submitting compliance identifiers for MiCA and CRS/CARF compliance.
 *
 * @property completed Whether all MiCA and CRS/CARF identifier requirements have been satisfied.
 * @property identifiers The MiCA identifiers that still need to be provided.
 * @property alternatives Alternative MiCA identifiers that can satisfy a requirement.
 * @property invalidIdentifiers Submitted identifiers that failed validation.
 * @property carfTinRequired Whether the customer still needs to provide at least one CRS/CARF TIN.
 */
@ExperimentalCryptoOnramp
@Poko
class SubmitIdentifiersResult internal constructor(
    val completed: Boolean,
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>,
    val invalidIdentifiers: List<ComplianceIdentifierType>,
    val carfTinRequired: Boolean
)
