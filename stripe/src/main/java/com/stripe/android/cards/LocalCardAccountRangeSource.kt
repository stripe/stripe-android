package com.stripe.android.cards

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata

/**
 * A [CardAccountRangeSource] that uses a local, static source of BIN ranges.
 */
internal class LocalCardAccountRangeSource : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return ACCOUNTS
            .firstOrNull {
                it.binRange.matches(cardNumber)
            }
    }

    internal companion object {
        private val VISA_ACCOUNTS =
            setOf(
                BinRange(
                    low = "4000000000000000",
                    high = "4999999999999999"
                )
            ).map {
                CardMetadata.AccountRange(
                    binRange = it,
                    panLength = 16,
                    brandName = CardBrand.Visa.code,
                    brand = CardBrand.Visa
                )
            }

        private val MASTERCARD_ACCOUNTS =
            setOf(
                BinRange(
                    low = "2221000000000000",
                    high = "2720999999999999"
                ),
                BinRange(
                    low = "5100000000000000",
                    high = "5599999999999999"
                )
            ).map {
                CardMetadata.AccountRange(
                    binRange = it,
                    panLength = 16,
                    brandName = CardBrand.MasterCard.code,
                    brand = CardBrand.MasterCard
                )
            }

        private val AMEX_ACCOUNTS = setOf(
            BinRange(
                low = "340000000000000",
                high = "349999999999999"
            ),

            BinRange(
                low = "370000000000000",
                high = "379999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 15,
                brandName = CardBrand.AmericanExpress.code,
                brand = CardBrand.AmericanExpress
            )
        }

        private val DISCOVER_ACCOUNTS = setOf(
            BinRange(
                low = "6000000000000000",
                high = "6099999999999999"
            ),

            BinRange(
                low = "6400000000000000",
                high = "6499999999999999"
            ),

            BinRange(
                low = "6500000000000000",
                high = "6599999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 16,
                brandName = CardBrand.Discover.code,
                brand = CardBrand.Discover
            )
        }

        private val JCB_ACCOUNTS = setOf(
            BinRange(
                low = "3528000000000000",
                high = "3589999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 16,
                brandName = CardBrand.JCB.code,
                brand = CardBrand.JCB
            )
        }

        private val UNIONPAY_ACCOUNTS = setOf(
            BinRange(
                low = "6200000000000000",
                high = "6299999999999999"
            ),

            BinRange(
                low = "8100000000000000",
                high = "8199999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 16,
                brandName = CardBrand.UnionPay.code,
                brand = CardBrand.UnionPay
            )
        }

        private val DINERSCLUB16_ACCOUNT_RANGES = setOf(
            BinRange(
                low = "3000000000000000",
                high = "3059999999999999"
            ),

            BinRange(
                low = "3095000000000000",
                high = "3095999999999999"
            ),

            BinRange(
                low = "3800000000000000",
                high = "3999999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 16,
                brandName = CardBrand.DinersClub.code,
                brand = CardBrand.DinersClub
            )
        }

        private val DINERSCLUB14_ACCOUNT_RANGES = setOf(
            BinRange(
                low = "36000000000000",
                high = "36999999999999"
            )
        ).map {
            CardMetadata.AccountRange(
                binRange = it,
                panLength = 14,
                brandName = CardBrand.DinersClub.code,
                brand = CardBrand.DinersClub
            )
        }

        internal val ACCOUNTS =
            VISA_ACCOUNTS
                .plus(MASTERCARD_ACCOUNTS)
                .plus(AMEX_ACCOUNTS)
                .plus(DISCOVER_ACCOUNTS)
                .plus(JCB_ACCOUNTS)
                .plus(UNIONPAY_ACCOUNTS)
                .plus(DINERSCLUB16_ACCOUNT_RANGES)
                .plus(DINERSCLUB14_ACCOUNT_RANGES)
    }
}
