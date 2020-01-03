package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CvcTokenParams(private val cvc: String) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.CVC_UPDATE to mapOf("cvc" to cvc)
        )
    }
}
