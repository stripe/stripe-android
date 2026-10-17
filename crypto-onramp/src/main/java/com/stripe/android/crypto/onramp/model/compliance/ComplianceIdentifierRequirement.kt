package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * A compliance identifier the customer still needs to provide.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierRequirement internal constructor(
    val type: ComplianceIdentifierType,
    val regulation: ComplianceRegulation
)
