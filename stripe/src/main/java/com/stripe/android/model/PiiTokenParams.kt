package com.stripe.android.model

import kotlinx.parcelize.Parcelize

/**
 * [TokenParams] for creating a PII token.
 */
@Parcelize
internal data class PiiTokenParams(
    private val personalId: String
) : TokenParams(Token.Type.Pii) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf("personal_id_number" to personalId)
}
