package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo

abstract class TokenParams(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val tokenType: Token.Type,
    /**
     * The SDK components that were involved in the creation of this token
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val attribution: Set<String> = emptySet()
) : StripeParamsModel, Parcelable {
    abstract val typeDataParams: Map<String, Any>

    override fun toParamMap(): Map<String, Any> = mapOf(tokenType.code to typeDataParams)
}
