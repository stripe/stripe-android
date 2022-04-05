package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.cards.CardNumber
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class BinRange(
    val low: String,
    val high: String
) : StripeModel {
    /**
     * Number matching strategy: Truncate the longer of the two numbers (theirs and our
     * bounds) to match the length of the shorter one, then do numerical compare.
     */
    fun matches(cardNumber: CardNumber.Unvalidated): Boolean {
        val number = cardNumber.normalized
        val numberBigDecimal = number.toBigDecimalOrNull() ?: return false

        val withinLowRange = if (number.length < low.length) {
            numberBigDecimal >= low.take(number.length).toBigDecimal()
        } else {
            number.take(low.length).toBigDecimal() >= low.toBigDecimal()
        }

        val withinHighRange = if (number.length < high.length) {
            numberBigDecimal <= high.take(number.length).toBigDecimal()
        } else {
            number.take(high.length).toBigDecimal() <= high.toBigDecimal()
        }
        return withinLowRange && withinHighRange
    }
}
