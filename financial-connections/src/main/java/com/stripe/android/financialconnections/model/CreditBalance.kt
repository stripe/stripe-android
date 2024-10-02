package com.stripe.android.financialconnections.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param used The credit that has been used by the account holder.
 * Each key is a three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html), in lowercase.
 * Each value is a integer amount.
 * A positive amount indicates money owed to the account holder.
 * A negative amount indicates money owed by the account holder.
 */
@Serializable
@Parcelize
@Poko
class CreditBalance internal constructor(
    @SerialName("used")
    val used: Map<String, Int>? = null

) : Parcelable
