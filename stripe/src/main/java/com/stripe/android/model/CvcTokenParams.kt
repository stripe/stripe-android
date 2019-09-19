package com.stripe.android.model

data class CvcTokenParams(private val cvc: String) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.CVC_UPDATE to mapOf("cvc" to cvc)
        )
    }
}
