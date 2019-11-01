package com.stripe.android.model

import androidx.annotation.DrawableRes
import com.stripe.android.R

enum class CardBrand(
    val code: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    val maxLengthWithSpaces: Int = 19,
    val maxLengthWithoutSpaces: Int = 16,

    /**
     * Based on [Issuer identification number table](http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29)
     */
    private val prefixes: Set<String> = emptySet()
) {
    AmericanExpress(
        "amex",
        "American Express",
        R.drawable.stripe_ic_amex,
        maxLengthWithSpaces = 17,
        maxLengthWithoutSpaces = 15,
        prefixes = setOf("34", "37")
    ),

    Discover(
        "discover",
        "Discover",
        R.drawable.stripe_ic_discover,
        prefixes = setOf("60", "64", "65")
    ),

    JCB(
        "jcb",
        "JCB",
        R.drawable.stripe_ic_jcb,
        prefixes = setOf("35")
    ),

    DinersClub(
        "diners",
        "Diners Club",
        R.drawable.stripe_ic_diners,
        maxLengthWithSpaces = 17,
        maxLengthWithoutSpaces = 14,
        prefixes = setOf(
            "300", "301", "302", "303", "304", "305", "309", "36", "38", "39"
        )
    ),

    Visa(
        "visa",
        "Visa",
        R.drawable.stripe_ic_visa,
        prefixes = setOf("4")
    ),

    MasterCard(
        "mastercard",
        "MasterCard",
        R.drawable.stripe_ic_mastercard,
        prefixes = setOf(
            "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229", "223", "224",
            "225", "226", "227", "228", "229", "23", "24", "25", "26", "270", "271", "2720",
            "50", "51", "52", "53", "54", "55", "67"
        )
    ),

    UnionPay(
        "unionpay",
        "UnionPay",
        R.drawable.stripe_ic_unionpay,
        prefixes = setOf("62")
    ),

    Unknown(
        "unknown",
        "Unknown",
        R.drawable.stripe_ic_unknown
    );

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
         * @param code a brand code, such as `visa` or `amex`. See [PaymentMethod.Card.brand].
         */
        fun fromCode(code: String?): CardBrand {
            return values().firstOrNull { it.code == code } ?: Unknown
        }
    }
}
