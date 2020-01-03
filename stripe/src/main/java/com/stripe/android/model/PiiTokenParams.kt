package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PiiTokenParams(
    private val personalId: String
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            Token.TokenType.PII to mapOf("personal_id_number" to personalId)
        )
    }
}
