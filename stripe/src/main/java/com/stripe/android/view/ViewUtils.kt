package com.stripe.android.view

import com.stripe.android.model.Card
import com.stripe.android.model.Card.Companion.CVC_LENGTH_AMERICAN_EXPRESS
import com.stripe.android.model.Card.Companion.CVC_LENGTH_COMMON

/**
 * Static utility functions needed for View classes.
 */
internal object ViewUtils {
    @JvmStatic
    fun isCvcMaximalLength(
        @Card.CardBrand cardBrand: String,
        cvcText: String?
    ): Boolean {
        val cvcLength = cvcText?.trim { it <= ' ' }?.length ?: 0
        return if (Card.CardBrand.AMERICAN_EXPRESS == cardBrand) {
            cvcLength == CVC_LENGTH_AMERICAN_EXPRESS
        } else {
            cvcLength == CVC_LENGTH_COMMON
        }
    }

    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     * Note that this does not verify that the card number is valid, or even that it is a number.
     *
     * @param spacelessCardNumber the raw card number, without spaces
     * @param brand the [Card.CardBrand] to use as a separating scheme
     * @return an array of strings with the number groups, in order. If the number is not complete,
     * some of the array entries may be `null`.
     */
    @JvmStatic
    fun separateCardNumberGroups(
        spacelessCardNumber: String,
        @Card.CardBrand brand: String
    ): Array<String?> {
        return if (brand == Card.CardBrand.AMERICAN_EXPRESS) {
            separateAmexCardNumberGroups(spacelessCardNumber.take(16))
        } else {
            separateDefaultCardNumberGroups(spacelessCardNumber.take(16))
        }
    }

    private fun separateDefaultCardNumberGroups(spacelessCardNumber: String): Array<String?> {
        val numberGroups = arrayOfNulls<String?>(4)
        var i = 0
        var previousStart = 0
        while ((i + 1) * 4 < spacelessCardNumber.length) {
            val group = spacelessCardNumber.substring(previousStart, (i + 1) * 4)
            numberGroups[i] = group
            previousStart = (i + 1) * 4
            i++
        }
        // Always stuff whatever is left into the next available array entry. This handles
        // incomplete numbers, full 16-digit numbers, and full 14-digit numbers
        numberGroups[i] = spacelessCardNumber.substring(previousStart)

        return numberGroups
    }

    private fun separateAmexCardNumberGroups(spacelessCardNumber: String): Array<String?> {
        val numberGroups = arrayOfNulls<String?>(3)

        val length = spacelessCardNumber.length
        var lastUsedIndex = 0
        if (length > 4) {
            numberGroups[0] = spacelessCardNumber.substring(0, 4)
            lastUsedIndex = 4
        }

        if (length > 10) {
            numberGroups[1] = spacelessCardNumber.substring(4, 10)
            lastUsedIndex = 10
        }

        for (i in 0..2) {
            if (numberGroups[i] != null) {
                continue
            }
            numberGroups[i] = spacelessCardNumber.substring(lastUsedIndex)
            break
        }

        return numberGroups
    }
}
