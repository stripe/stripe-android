package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
data class CvcTokenParams(
    private val cvc: String
) : TokenParams(Token.Type.CvcUpdate) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf("cvc" to cvc)
}
