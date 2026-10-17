package com.stripe.android.model

import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class CvcTokenParams(
    private val cvc: String
) : TokenParams(Token.Type.CvcUpdate) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf("cvc" to cvc)
}
