package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
data class CvcTokenParams(
    private val cvc: String
) : TokenParams(Token.TokenType.CVC_UPDATE) {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.CVC_UPDATE to mapOf("cvc" to cvc)
        )
    }
}
