package com.stripe.android.model

import android.os.Parcelable

abstract class TokenParams(
    internal val tokenType: Token.Type,
    /**
     * The SDK components that were involved in the creation of this token
     */
    internal val attribution: Set<String> = emptySet()
) : StripeParamsModel, Parcelable {
    abstract val typeDataParams: Map<String, Any>

    override fun toParamMap(): Map<String, Any> = mapOf(tokenType.code to typeDataParams)
}
