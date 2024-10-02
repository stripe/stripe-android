package com.stripe.android.financialconnections.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param asOf The time that the external institution calculated this balance.
 * Measured in seconds since the Unix epoch.
 * @param current The balances owed to (or by) the account holder.
 * Each key is a three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html),
 * in lowercase. Each value is a integer amount.
 * A positive amount indicates money owed to the account holder.
 * A negative amount indicates money owed by the account holder.
 * @param type
 * @param cash
 * @param credit
 */
@Parcelize
@Serializable
@Suppress("unused")
@Poko
class Balance internal constructor(

    /* The time that the external institution calculated this balance.
     Measured in seconds since the Unix epoch. */
    @SerialName("as_of")
    val asOf: Int,

    /* The balances owed to (or by) the account holder.
     Each key is a three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html),
     in lowercase. Each value is a integer amount.
     A positive amount indicates money owed to the account holder.
     A negative amount indicates money owed by the account holder.
     */
    @SerialName("current")
    val current: Map<String, Int>,

    @SerialName("type")
    val type: Type = Type.UNKNOWN,

    @SerialName("cash")
    val cash: CashBalance? = null,

    @SerialName("credit")
    val credit: CreditBalance? = null

) : Parcelable {

    @Serializable
    enum class Type(val value: String) {
        @SerialName("cash")
        CASH("cash"),

        @SerialName("credit")
        CREDIT("credit"),

        UNKNOWN("unknown")
    }
}
