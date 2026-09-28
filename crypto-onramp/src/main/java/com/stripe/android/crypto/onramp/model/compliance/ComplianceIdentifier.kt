package com.stripe.android.crypto.onramp.model.compliance

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * A compliance identifier collected for MiCA or CRS/CARF compliance.
 *
 * @property type The type of identifier provided.
 * @property value The identifier value.
 */
@ExperimentalCryptoOnramp
class ComplianceIdentifier {
    private var type: ComplianceIdentifierType? = null
    private var value: String? = null

    /**
     * Sets the identifier type.
     */
    fun type(type: ComplianceIdentifierType) = apply {
        this.type = type
    }

    /**
     * Sets the identifier value.
     */
    fun value(value: String) = apply {
        this.value = value
    }

    internal class State(
        val type: ComplianceIdentifierType,
        val value: String
    )

    internal fun build(): State {
        return State(
            type = requireNotNull(type) {
                "type must not be null"
            },
            value = requireNotNull(value) {
                "value must not be null"
            }
        )
    }
}
