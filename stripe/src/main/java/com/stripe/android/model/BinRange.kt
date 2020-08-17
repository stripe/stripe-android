package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class BinRange(
    val low: String,
    val high: String
) : StripeModel {
    /**
     *  Number matching strategy: Truncate the longer of the two numbers (theirs and our
     *  bounds) to match the length of the shorter one, then do numerical compare.
     */
    internal fun matches(number: String): Boolean {
        val withinLowRange = if (number.length < low.length) {
            number.toBigDecimal() >= low.take(number.length).toBigDecimal()
        } else {
            number.take(low.length).toBigDecimal() >= low.toBigDecimal()
        }

        val withinHighRange = if (number.length < high.length) {
            number.toBigDecimal() <= high.take(number.length).toBigDecimal()
        } else {
            number.take(high.length).toBigDecimal() <= high.toBigDecimal()
        }
        return withinLowRange && withinHighRange
    }
}
