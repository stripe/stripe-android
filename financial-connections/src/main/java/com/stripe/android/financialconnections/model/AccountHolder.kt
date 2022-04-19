package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param type
 * @param account
 * @param customer ID of the Customer this LinkedAccount belongs to. Present if and only if `type` is `customer`.
 */
@Parcelize
@Serializable
@Suppress("unused")
data class AccountHolder(

    @SerialName("type")
    val type: Type = Type.UNKNOWN,

    @SerialName("account")
    val account: String? = null,

    /* ID of the Customer this LinkedAccount belongs to. Present if and only if `type` is `customer`. */
    @SerialName("customer")
    val customer: String? = null

) : StripeModel, Parcelable {

    @Serializable
    enum class Type(val value: String) {
        @SerialName("account")
        ACCOUNT("account"),

        @SerialName("customer")
        CUSTOMER("customer"),

        UNKNOWN("unknown");
    }
}
