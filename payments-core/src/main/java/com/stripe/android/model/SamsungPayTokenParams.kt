package com.stripe.android.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Poko
class SamsungPayTokenParams(
    private val token: String
) : TokenParams(Token.Type.Card) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf(
            PARAM_WALLET to mapOf(
                PARAM_TYPE to WALLET_TYPE,
                PARAM_SAMSUNG_PAY to mapOf(
                    PARAM_TOKEN to token
                )
            )
        )

    private companion object {
        private const val PARAM_WALLET = "wallet"
        private const val PARAM_TYPE = "type"
        private const val PARAM_SAMSUNG_PAY = "samsung_pay"
        private const val PARAM_TOKEN = "token"
        private const val WALLET_TYPE = "samsung_pay"
    }
}
