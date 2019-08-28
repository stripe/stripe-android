package com.stripe.android

import com.stripe.android.model.Card
import com.stripe.android.model.Card.CardBrand

/**
 * Utility class for functions to do with cards.
 */
internal object CardUtils {

    private const val LENGTH_COMMON_CARD = 16
    private const val LENGTH_AMERICAN_EXPRESS = 15
    private const val LENGTH_DINERS_CLUB = 14

    /**
     * Returns a [CardBrand] corresponding to a partial card number,
     * or [Card.CardBrand.UNKNOWN] if the card brand can't be determined from the input value.
     *
     * @param cardNumber a credit card number or partial card number
     * @return the [Card.CardBrand] corresponding to that number,
     * or [CardBrand.UNKNOWN] if it can't be determined
     */
    @Card.CardBrand
    @JvmStatic
    fun getPossibleCardType(cardNumber: String?): String {
        return getPossibleCardType(cardNumber, true)
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
    @JvmStatic
    fun isValidLuhnNumber(cardNumber: String?): Boolean {
        if (cardNumber == null) {
            return false
        }

        val cardNumberReversed = cardNumber.toList().reversed()
        if (!cardNumberReversed.all { Character.isDigit(it) }) {
            return false
        }

        val sum = cardNumberReversed.mapIndexed { idx, c ->
            val digitInteger = Character.getNumericValue(c) *
                if (idx % 2 == 1) {
                    2
                } else {
                    1
                }

            if (digitInteger > 9) {
                digitInteger - 9
            } else {
                digitInteger
            }
        }.sum()

        return sum % 10 == 0
    }

    /**
     * Checks to see whether the input number is of the correct length, after determining its brand.
     * This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @return `true` if the card number is of known type and the correct length
     */
    @JvmStatic
    fun isValidCardLength(cardNumber: String?): Boolean {
        return cardNumber != null && isValidCardLength(cardNumber,
            getPossibleCardType(cardNumber, false))
    }

    /**
     * Checks to see whether the input number is of the correct length, given the assumed brand of
     * the card. This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @param cardBrand a [Card.CardBrand] used to get the correct size
     * @return `true` if the card number is the correct length for the assumed brand
     */
    @JvmStatic
    fun isValidCardLength(cardNumber: String?, @CardBrand cardBrand: String): Boolean {
        if (cardNumber == null || CardBrand.UNKNOWN == cardBrand) {
            return false
        }

        val length = cardNumber.length
        return when (cardBrand) {
            CardBrand.AMERICAN_EXPRESS -> length == LENGTH_AMERICAN_EXPRESS
            CardBrand.DINERS_CLUB -> length == LENGTH_DINERS_CLUB
            else -> length == LENGTH_COMMON_CARD
        }
    }

    @CardBrand
    private fun getPossibleCardType(cardNumber: String?, shouldNormalize: Boolean): String {
        if (StripeTextUtils.isBlank(cardNumber)) {
            return CardBrand.UNKNOWN
        }

        val spacelessCardNumber: String? = if (shouldNormalize) {
            StripeTextUtils.removeSpacesAndHyphens(cardNumber)
        } else {
            cardNumber
        }

        return when {
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_AMERICAN_EXPRESS) ->
                CardBrand.AMERICAN_EXPRESS
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_DISCOVER) ->
                CardBrand.DISCOVER
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_JCB) ->
                CardBrand.JCB
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_DINERS_CLUB) ->
                CardBrand.DINERS_CLUB
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_VISA) ->
                CardBrand.VISA
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_MASTERCARD) ->
                CardBrand.MASTERCARD
            StripeTextUtils.hasAnyPrefix(spacelessCardNumber, *Card.PREFIXES_UNIONPAY) ->
                CardBrand.UNIONPAY
            else -> CardBrand.UNKNOWN
        }
    }
}
