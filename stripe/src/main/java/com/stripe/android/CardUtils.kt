package com.stripe.android

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
    @JvmStatic
    fun getPossibleCardBrand(cardNumber: String?): CardBrand {
        return getPossibleCardBrand(cardNumber, true)
    }

    /**
     * Checks the input string to see whether or not it is a valid card number, possibly
     * with groupings separated by spaces or hyphens.
     *
     * @param cardNumber a String that may or may not represent a valid card number
     * @return `true` if and only if the input value is a valid card number
     */
    @JvmStatic
    fun isValidCardNumber(cardNumber: String?): Boolean {
        val normalizedNumber = StripeTextUtils.removeSpacesAndHyphens(cardNumber)
        return isValidLuhnNumber(normalizedNumber) && isValidCardLength(normalizedNumber)
    }

    /**
     * Checks the input string to see whether or not it is a valid Luhn number.
     *
     * @param cardNumber a String that may or may not represent a valid Luhn number
     * @return `true` if and only if the input value is a valid Luhn number
     */
    internal fun isValidLuhnNumber(cardNumber: String?): Boolean {
        if (cardNumber == null) {
            return false
        }

        var isOdd = true
        var sum = 0

        for (index in cardNumber.length - 1 downTo 0) {
            val c = cardNumber[index]
            if (!Character.isDigit(c)) {
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

    /**
     * Checks to see whether the input number is of the correct length, after determining its brand.
     * This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @return `true` if the card number is of known type and the correct length
     */
    internal fun isValidCardLength(cardNumber: String?): Boolean {
        return cardNumber != null &&
            getPossibleCardBrand(cardNumber, false).isValidCardNumberLength(cardNumber)
    }

    private fun getPossibleCardBrand(cardNumber: String?, shouldNormalize: Boolean): CardBrand {
        if (cardNumber.isNullOrBlank()) {
            return CardBrand.Unknown
        }

        val spacelessCardNumber =
            if (shouldNormalize) {
                StripeTextUtils.removeSpacesAndHyphens(cardNumber)
            } else {
                cardNumber
            }

        return CardBrand.fromCardNumber(spacelessCardNumber)
    }
}
