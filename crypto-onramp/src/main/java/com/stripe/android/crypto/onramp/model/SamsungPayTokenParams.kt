package com.stripe.android.crypto.onramp.model

import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import kotlinx.parcelize.Parcelize

@Parcelize
internal class SamsungPayTokenParams(
    private val paymentCredential: String,
) : TokenParams(
    tokenType = Token.Type.Card,
    attribution = setOf("samsung_pay"),
) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf(
            "wallet" to mapOf(
                "type" to "samsung_pay",
                "samsung_pay" to mapOf(
                    "token" to paymentCredential,
                ),
            ),
        )
}
