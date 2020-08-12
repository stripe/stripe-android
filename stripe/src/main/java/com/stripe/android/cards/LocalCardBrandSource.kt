package com.stripe.android.cards

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand

/**
 * A [CardBrandSource] that uses a local, static source of BIN ranges.
 */
internal class LocalCardBrandSource : CardBrandSource {
    override fun getCardBrand(cardNumber: String): CardBrand {
        return BIN_RANGES
            .filterKeys { binRange -> binRange.matches(cardNumber) }
            .values
            .firstOrNull() ?: CardBrand.Unknown
    }

    private companion object {
        private val VISA_BIN_RANGES = mapOf(
            BinRange(
                low = "4000000000000000",
                high = "4999999999999999"
            ) to CardBrand.Visa
        )

        private val MASTERCARD_BIN_RANGES = mapOf(
            BinRange(
                low = "2221000000000000",
                high = "2720999999999999"
            ) to CardBrand.MasterCard,

            BinRange(
                low = "5100000000000000",
                high = "5599999999999999"
            ) to CardBrand.MasterCard
        )

        private val AMEX_BIN_RANGES = mapOf(
            BinRange(
                low = "340000000000000",
                high = "349999999999999"
            ) to CardBrand.AmericanExpress,

            BinRange(
                low = "370000000000000",
                high = "379999999999999"
            ) to CardBrand.AmericanExpress
        )

        private val DISCOVER_BIN_RANGES = mapOf(
            BinRange(
                low = "6000000000000000",
                high = "6099999999999999"
            ) to CardBrand.Discover,

            BinRange(
                low = "6400000000000000",
                high = "6499999999999999"
            ) to CardBrand.Discover,

            BinRange(
                low = "6500000000000000",
                high = "6599999999999999"
            ) to CardBrand.Discover
        )

        private val JCB_BIN_RANGES = mapOf(
            BinRange(
                low = "3528000000000000",
                high = "3589999999999999"
            ) to CardBrand.JCB
        )

        private val UNIONPAY_BIN_RANGES = mapOf(
            BinRange(
                low = "6200000000000000",
                high = "6299999999999999"
            ) to CardBrand.UnionPay,

            BinRange(
                low = "8100000000000000",
                high = "8199999999999999"
            ) to CardBrand.UnionPay
        )

        private val DINERSCLUB_BIN_RANGES = mapOf(
            // 14 digits
            BinRange(
                low = "36000000000000",
                high = "36999999999999"
            ) to CardBrand.DinersClub,

            BinRange(
                low = "3000000000000000",
                high = "3059999999999999"
            ) to CardBrand.DinersClub,

            BinRange(
                low = "3095000000000000",
                high = "3095999999999999"
            ) to CardBrand.DinersClub,

            BinRange(
                low = "3800000000000000",
                high = "3999999999999999"
            ) to CardBrand.DinersClub
        )

        private val BIN_RANGES =
            VISA_BIN_RANGES
                .plus(MASTERCARD_BIN_RANGES)
                .plus(AMEX_BIN_RANGES)
                .plus(DISCOVER_BIN_RANGES)
                .plus(JCB_BIN_RANGES)
                .plus(UNIONPAY_BIN_RANGES)
                .plus(DINERSCLUB_BIN_RANGES)
    }
}
