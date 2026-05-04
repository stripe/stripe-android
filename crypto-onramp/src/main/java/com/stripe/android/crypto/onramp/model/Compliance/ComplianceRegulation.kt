package com.stripe.android.crypto.onramp.model.Compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * The regulation requiring a compliance identifier.
 */
@ExperimentalCryptoOnramp
enum class ComplianceRegulation(val value: String) {
    EuCarf("eu_carf"),
    EuMica("eu_mica");

    companion object {
        fun fromValue(value: String): ComplianceRegulation? {
            return entries.firstOrNull { it.value == value }
        }
    }
}
