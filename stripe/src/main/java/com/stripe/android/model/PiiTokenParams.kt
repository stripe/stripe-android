package com.stripe.android.model

data class PiiTokenParams(private val personalId: String) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.PII to mapOf("personal_id_number" to personalId)
        )
    }
}
