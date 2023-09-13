package com.stripe.android.model

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.stripe.android.cards.CardNumber
import com.stripe.payments.model.R
import java.util.regex.Pattern

/**
 * A representation of supported card brands and related data
 */
@Suppress("LongParameterList", "MaxLineLength")
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
    private val defaultMaxLength: Int = 16,

    /**
     * Based on [Issuer identification number table](http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29)
     */
    private val pattern: Pattern? = null,

    /**
     * Patterns for discrete lengths
     */
    private val partialPatterns: Map<Int, Pattern>,

    /**
     * By default, a [CardBrand] does not have variants.
     */
    private val variantMaxLength: Map<Pattern, Int> = emptyMap(),

    /**
     * Whether the brand should be rendered
     */
    private val shouldRender: Boolean = true,

    /**
     * The rendering order in the card details cell
     */
    private val renderingOrder: Int,
) {
    Visa(
        "visa",
        "Visa",
        R.drawable.stripe_ic_visa,
        pattern = Pattern.compile("^(4)[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^4$")
        ),
        renderingOrder = 1
    ),

    MasterCard(
        "mastercard",
        "Mastercard",
        R.drawable.stripe_ic_mastercard,
        pattern = Pattern.compile(
            "^(2221|2222|2223|2224|2225|2226|2227|2228|2229|222|223|224|225|226|" +
                "227|228|229|23|24|25|26|270|271|2720|50|51|52|53|54|55|56|57|58|59|67)[0-9]*$"
        ),
        partialPatterns = mapOf(
            1 to Pattern.compile("^2|5|6$"),
            2 to Pattern.compile("^(22|23|24|25|26|27|50|51|52|53|54|55|56|57|58|59|67)$")
        ),
        renderingOrder = 2
    ),

    AmericanExpress(
        "amex",
        "American Express",
        R.drawable.stripe_ic_amex,
        cvcIcon = R.drawable.stripe_ic_cvc_amex,
        cvcLength = setOf(3, 4),
        defaultMaxLength = 15,
        pattern = Pattern.compile("^(34|37)[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^3$")
        ),
        renderingOrder = 3
    ),

    Discover(
        "discover",
        "Discover",
        R.drawable.stripe_ic_discover,
        pattern = Pattern.compile("^(60|64|65)[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^6$")
        ),
        renderingOrder = 4
    ),

    /**
     * JCB
     *
     * BIN range: 352800 to 358999
     */
    JCB(
        "jcb",
        "JCB",
        R.drawable.stripe_ic_jcb,
        pattern = Pattern.compile("^(352[89]|35[3-8][0-9])[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^3$"),
            2 to Pattern.compile("^(35)$"),
            3 to Pattern.compile("^(35[2-8])$")
        ),
        renderingOrder = 5
    ),

    /**
     * Diners Club
     *
     * 14-digits: BINs starting with 36
     * 16-digits: BINs starting with 30, 38, 39
     */
    DinersClub(
        "diners",
        "Diners Club",
        R.drawable.stripe_ic_diners,
        defaultMaxLength = 16,
        pattern = Pattern.compile("^(36|30|38|39)[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^3$")
        ),
        variantMaxLength = mapOf(
            Pattern.compile("^(36)[0-9]*$") to 14
        ),
        renderingOrder = 6
    ),

    UnionPay(
        "unionpay",
        "UnionPay",
        R.drawable.stripe_ic_unionpay,
        pattern = Pattern.compile("^(62|81)[0-9]*$"),
        partialPatterns = mapOf(
            1 to Pattern.compile("^6|8$")
        ),
        renderingOrder = 7
    ),

    CartesBancaires(
        "cartes_bancaires",
        "Cartes Bancaires",
        R.drawable.stripe_ic_cartes_bancaires,
        pattern = Pattern.compile(
            "(^(4)[0-9]*) |" +
                "^(2221|2222|2223|2224|2225|2226|2227|2228|2229|222|223|224|225|226|" +
                "227|228|229|23|24|25|26|270|271|2720|50|51|52|53|54|55|56|57|58|59|67)[0-9]*$"
        ),
        partialPatterns = mapOf(
            1 to Pattern.compile("^4$"),
            2 to Pattern.compile("^2|5|6$"),
            3 to Pattern.compile("^(22|23|24|25|26|27|50|51|52|53|54|55|56|57|58|59|67)$")
        ),
        shouldRender = false,
        renderingOrder = 8
    ),

    Unknown(
        "unknown",
        "Unknown",
        R.drawable.stripe_ic_unknown,
        cvcLength = setOf(3, 4),
        partialPatterns = emptyMap(),
        renderingOrder = -1
    );

    val maxCvcLength: Int
        get() {
            return cvcLength.maxOrNull() ?: CVC_COMMON_LENGTH
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getMaxLengthForCardNumber(cardNumber: String): Int {
        val normalizedCardNumber = CardNumber.Unvalidated(cardNumber).normalized
        return variantMaxLength.entries.firstOrNull { (pattern, _) ->
            pattern.matcher(normalizedCardNumber).matches()
        }?.value ?: defaultMaxLength
    }

    private fun getPatternForLength(cardNumber: String): Pattern? {
        return partialPatterns[cardNumber.length] ?: pattern
    }

    companion object {
        /**
         * @param cardNumber a card number
         * @return the [CardBrand] that matches the [cardNumber]'s prefix, if one is found;
         * otherwise, [CardBrand.Unknown]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromCardNumber(cardNumber: String?): CardBrand {
            if (cardNumber.isNullOrBlank()) {
                return Unknown
            }

            // Only return a card brand if we know exactly which one, if there is more than
            // one possibility return unknown
            return (
                getMatchingCards(cardNumber).takeIf {
                    it.size == 1
                } ?: listOf(Unknown)
                ).first()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun getCardBrands(cardNumber: String?): List<CardBrand> {
            if (cardNumber.isNullOrBlank()) {
                return orderedBrands
            }

            return getMatchingCards(cardNumber).takeIf {
                it.isNotEmpty()
            } ?: listOf(Unknown)
        }

        private fun getMatchingCards(cardNumber: String) = values().filter { cardBrand ->
            cardBrand.getPatternForLength(cardNumber)?.matcher(cardNumber)
                ?.matches() == true
        }.filter {
            it.shouldRender
        }

        /**
         * @param code a brand code, such as `Visa` or `American Express`.
         * See [PaymentMethod.Card.brand].
         */
        fun fromCode(code: String?): CardBrand {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Unknown
        }

        val orderedBrands = values()
            .toList()
            .filter { it.shouldRender }
            .filter { it.renderingOrder > 0 }
            .sortedBy { it.renderingOrder }

        private const val CVC_COMMON_LENGTH: Int = 3
    }
}
