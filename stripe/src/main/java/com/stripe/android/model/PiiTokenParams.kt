package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * [TokenParams] for creating a PII token.
 */
@Parcelize
internal data class PiiTokenParams(
    private val personalId: String
) : TokenParams(Token.TokenType.PII) {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.PII to mapOf("personal_id_number" to personalId)
        )
    }
}
