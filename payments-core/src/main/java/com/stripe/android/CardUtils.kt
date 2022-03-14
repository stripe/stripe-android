package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.CardBrand

/**
 * Utility class for functions to do with cards.
 */
object CardUtils {

    /**
     * @param cardNumber a full or partial card number
     * @return the [CardBrand] that matches the card number based on prefixes,
     * or [CardBrand.Unknown] if it can't be determined
     */
    @Deprecated(
        "CardInputWidget and CardMultilineWidget handle card brand lookup. " +
            "This method should not be relied on for determining CardBrand."
    )
    @JvmStatic
    fun getPossibleCardBrand(cardNumber: String?): CardBrand {
        return if (cardNumber.isNullOrBlank()) {
            CardBrand.Unknown
        } else {
            CardBrand.fromCardNumber(CardNumber.Unvalidated(cardNumber).normalized)
        }
    }

    /**
     * Checks the input string to see whether or not it is a valid Luhn number.
     *
     * @param cardNumber a String that may or may not represent a valid Luhn number
     * @return `true` if and only if the input value is a valid Luhn number
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ReturnCount")
    fun isValidLuhnNumber(cardNumber: String?): Boolean {
        if (cardNumber == null) {
            return false
        }

        var isOdd = true
        var sum = 0

        for (index in cardNumber.length - 1 downTo 0) {
            val c = cardNumber[index]
            if (!c.isDigit()) {
                return false
            }

            var digitInteger = Character.getNumericValue(c)
            isOdd = !isOdd

            if (isOdd) {
                digitInteger *= 2
            }

            if (digitInteger > 9) {
                digitInteger -= 9
            }

            sum += digitInteger
        }

        return sum % 10 == 0
    }
}
