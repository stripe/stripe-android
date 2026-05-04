package com.stripe.android.crypto.onramp.model.Compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * A group describing alternative identifier types that may satisfy a requirement.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierAlternativeGroup internal constructor(
    val originalMissingIdentifiers: List<ComplianceIdentifierType>,
    val alternativeMissingIdentifiers: List<ComplianceIdentifierType>
)
