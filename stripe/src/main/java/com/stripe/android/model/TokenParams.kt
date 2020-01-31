package com.stripe.android.model

import android.os.Parcelable

abstract class TokenParams(
    @Token.TokenType internal val tokenType: String,
    /**
     * The SDK components that were involved in the creation of this token
     */
    internal val attribution: Collection<String> = emptySet()
) : StripeParamsModel, Parcelable
