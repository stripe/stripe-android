package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class CardMetadata internal constructor(
    val binPrefix: String,
    val accountRanges: List<AccountRange>
) : StripeModel {

    @Parcelize
    internal data class AccountRange internal constructor(
        val accountRangeHigh: String,
        val accountRangeLow: String,
        val panLength: Int,
        val brand: String,
        val country: String
    ) : StripeModel
}
