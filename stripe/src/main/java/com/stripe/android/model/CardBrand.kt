package com.stripe.android.model

import androidx.annotation.DrawableRes
import com.stripe.android.R

/**
 * A representation of supported card brands and related data
 */
enum class CardBrand(
    val code: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    @DrawableRes val cvcIcon: Int = R.drawable.stripe_ic_cvc,
    @DrawableRes val errorIcon: Int = R.drawable.stripe_ic_error,

    /**
     * Accepted CVC lengths
     */
    val cvcLength: Set<Int> = setOf(3),

    /**
     * The default max length when the card number is formatted without spaces (e.g. "4242424242424242")
     *
     * Note that [CardBrand.DinersClub]'s max length depends on the BIN (e.g. card number prefix).
     * In the case of a [CardBrand.DinersClub] card, use [getMaxLengthForCardNumber].
     */
    val defaultMaxLength: Int = 16,

    /**
     * Based on [Issuer identification number table](http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29)
     */
    val prefixes: List<String> = emptyList(),

    /**
     * The position of spaces in a formatted card number. For example, "4242424242424242" is
     * formatted to "4242 4242 4242 4242".
     */
    val defaultSpacePositions: Set<Int> = setOf(4, 9, 14),

    /**
     * By default, a [CardBrand] does not have variants.
     */
    private val variantMaxLength: Map<String, Int> = emptyMap(),

    private val variantSpacePositions: Map<String, Set<Int>> = emptyMap()
) {
    AmericanExpress(
        "amex",
        "American Express",
        R.drawable.stripe_ic_amex,
        cvcIcon = R.drawable.stripe_ic_cvc_amex,
        errorIcon = R.drawable.stripe_ic_error_amex,
        cvcLength = setOf(3, 4),
        defaultMaxLength = 15,
        prefixes = listOf("34", "37"),
        defaultSpacePositions = setOf(4, 11)
    ),

    Discover(
        "discover",
        "Discover",
        R.drawable.stripe_ic_discover,
        prefixes = listOf("60", "64", "65")
    ),

    JCB(
        "jcb",
        "JCB",
        R.drawable.stripe_ic_jcb,
        prefixes = listOf("35")
    ),

    // 14-digit Diners Club
    DinersClub(
        "diners",
        "Diners Club",
        R.drawable.stripe_ic_diners,
        defaultMaxLength = 16,
        prefixes = listOf(
            // 14-digits
            "36",

            // 16-digits
            "30", "38", "39"
        ),
        variantMaxLength = mapOf(
            "36" to 14
        ),
        variantSpacePositions = mapOf(
            "36" to setOf(4, 11)
        )
    ),

    Visa(
        "visa",
        "Visa",
        R.drawable.stripe_ic_visa,
        prefixes = listOf("4")
    ),

    MasterCard(
        "mastercard",
        "Mastercard",
        R.drawable.stripe_ic_mastercard,
        prefixes = listOf(
            "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229", "223", "224",
            "225", "226", "227", "228", "229", "23", "24", "25", "26", "270", "271", "2720",
            "50", "51", "52", "53", "54", "55", "67"
        )
    ),

    UnionPay(
        "unionpay",
        "UnionPay",
        R.drawable.stripe_ic_unionpay,
        prefixes = listOf("62")
    ),

    Unknown(
        "unknown",
        "Unknown",
        R.drawable.stripe_ic_unknown,
        cvcLength = setOf(3, 4)
    );

    val defaultMaxLengthWithSpaces: Int = defaultMaxLength + defaultSpacePositions.size

    val maxCvcLength: Int
        get() {
            return cvcLength.max() ?: CVC_COMMON_LENGTH
        }

    /**
     * Checks to see whether the input number is of the correct length, given the assumed brand of
     * the card. This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @return `true` if the card number is the correct length for the assumed brand
     */
    fun isValidCardNumberLength(cardNumber: String?): Boolean {
        return cardNumber != null && Unknown != this &&
            cardNumber.length == getMaxLengthForCardNumber(cardNumber)
    }

    fun isValidCvc(cvc: String): Boolean {
        return cvcLength.contains(cvc.length)
    }

    fun isMaxCvc(cvcText: String?): Boolean {
        val cvcLength = cvcText?.trim()?.length ?: 0
        return maxCvcLength == cvcLength
    }

    /**
     * If the [CardBrand] has variants, and the [cardNumber] starts with one of the variant
     * prefixes, return the length for that variant. Otherwise, return [defaultMaxLength].
     *
     * Note: currently only [CardBrand.DinersClub] has variants
     */
    fun getMaxLengthForCardNumber(cardNumber: String): Int {
        return variantMaxLength.entries.firstOrNull { (key, _) ->
            cardNumber.startsWith(key)
        }?.value ?: defaultMaxLength
    }

    fun getMaxLengthWithSpacesForCardNumber(cardNumber: String): Int {
        return getMaxLengthForCardNumber(cardNumber) +
            getSpacePositionsForCardNumber(cardNumber).size
    }

    /**
     * If the [CardBrand] has variants, and the [cardNumber] starts with one of the variant
     * prefixes, return the length for that variant. Otherwise, return [defaultMaxLength].
     *
     * Note: currently only [CardBrand.DinersClub] has variants
     */
    fun getSpacePositionsForCardNumber(cardNumber: String): Set<Int> {
        return variantSpacePositions.entries.firstOrNull { (key, _) ->
            cardNumber.startsWith(key)
        }?.value ?: defaultSpacePositions
    }

    /**
     * Format a number according to brand requirements.
     *
     * e.g. `"4242424242424242"` will return `"4242 4242 4242 4242"`
     */
    fun formatNumber(cardNumber: String): String {
        return groupNumber(cardNumber)
            .takeWhile { it != null }
            .joinToString(" ")
    }

    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     * Note that this does not verify that the card number is valid, or even that it is a number.
     *
     * e.g. `"4242424242424242"` will return `["4242", "4242", "4242", "4242"]`
     *
     * @param cardNumber the raw card number
     *
     * @return an array of strings with the number groups, in order. If the number is not complete,
     * some of the array entries may be `null`.
     */
    fun groupNumber(cardNumber: String): Array<String?> {
        val spacelessCardNumber = cardNumber.take(getMaxLengthForCardNumber(cardNumber))
        val spacePositions = getSpacePositionsForCardNumber(cardNumber)
        val groups = arrayOfNulls<String?>(spacePositions.size + 1)

        val length = spacelessCardNumber.length
        var lastUsedIndex = 0

        spacePositions
            .toList().sorted().forEachIndexed { idx, spacePosition ->
                val adjustedSpacePosition = spacePosition - idx
                if (length > adjustedSpacePosition) {
                    groups[idx] = spacelessCardNumber.substring(
                        lastUsedIndex,
                        adjustedSpacePosition
                    )
                    lastUsedIndex = adjustedSpacePosition
                }
            }

        // populate any remaining digits in the first index with a null value
        groups
            .indexOfFirst { it == null }
            .takeIf {
                it != -1
            }?.let {
                groups[it] = spacelessCardNumber.substring(lastUsedIndex)
            }

        return groups
    }

    companion object {
        /**
         * @param cardNumber a card number
         * @return the [CardBrand] that matches the [cardNumber]'s prefix, if one is found;
         * otherwise, [CardBrand.Unknown]
         */
        fun fromCardNumber(cardNumber: String?): CardBrand {
            return values()
                .firstOrNull { cardBrand ->
                    cardBrand.prefixes
                        .takeIf {
                            it.isNotEmpty()
                        }?.any {
                            cardNumber?.startsWith(it) == true
                        } == true
                } ?: Unknown
        }

        /**
         * @param code a brand code, such as `Visa` or `American Express`.
         * See [PaymentMethod.Card.brand].
         */
        fun fromCode(code: String?): CardBrand {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Unknown
        }

        private const val CVC_COMMON_LENGTH: Int = 3
    }
}
