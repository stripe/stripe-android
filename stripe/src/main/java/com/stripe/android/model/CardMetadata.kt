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
    ) : StripeModel {

        /**
         *  Number matching strategy: Truncate the longer of the two numbers (theirs and our
         *  bounds) to match the length of the shorter one, then do numerical compare.
         */
        internal fun matches(number: String): Boolean {
            val withinLowRange = if (number.length < this.accountRangeLow.length) {
                number.toBigDecimal() >= this.accountRangeLow.substring(0, number.length).toBigDecimal()
            } else {
                number.substring(0, this.accountRangeLow.length).toBigDecimal() >= this.accountRangeLow.toBigDecimal()
            }

            val withinHighRange = if (number.length < this.accountRangeHigh.length) {
                number.toBigDecimal() <= this.accountRangeHigh.substring(0, number.length).toBigDecimal()
            } else {
                number.substring(0, this.accountRangeHigh.length).toBigDecimal() <= this.accountRangeHigh.toBigDecimal()
            }
            return withinLowRange && withinHighRange
        }
    }
}
