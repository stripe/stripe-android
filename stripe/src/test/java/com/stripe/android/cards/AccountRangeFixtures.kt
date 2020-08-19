package com.stripe.android.cards

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata

internal object AccountRangeFixtures {
    val VISA = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "4242424240000000",
            high = "4242424249999999"
        ),
        panLength = 16,
        brandInfo = CardMetadata.AccountRange.BrandInfo.Visa,
        country = "GB"
    )

    val AMERICANEXPRESS = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "378282000000000",
            high = "378282999999999"
        ),
        panLength = 15,
        brandInfo = CardMetadata.AccountRange.BrandInfo.AmericanExpress,
        country = "US"
    )

    val MASTERCARD = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "5100000000000000",
            high = "5599999999999999"
        ),
        panLength = 16,
        brandInfo = CardMetadata.AccountRange.BrandInfo.Mastercard
    )

    val JCB = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "3528000000000000",
            high = "3589999999999999"
        ),
        panLength = 16,
        brandInfo = CardMetadata.AccountRange.BrandInfo.JCB
    )

    val DINERSCLUB14 = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "36000000000000",
            high = "36999999999999"
        ),
        panLength = 14,
        brandInfo = CardMetadata.AccountRange.BrandInfo.DinersClub
    )

    val DINERSCLUB16 = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "3000000000000000",
            high = "3059999999999999"
        ),
        panLength = 16,
        brandInfo = CardMetadata.AccountRange.BrandInfo.DinersClub
    )

    val UNIONPAY19 = CardMetadata.AccountRange(
        binRange = BinRange(
            low = "6216828050000000000",
            high = "6216828059999999999"
        ),
        panLength = 19,
        brandInfo = CardMetadata.AccountRange.BrandInfo.UnionPay,
        country = "CN"
    )

    val DEFAULT = listOf(
        CardMetadata.AccountRange(
            binRange = BinRange(
                low = "4242420000000000",
                high = "4242424239999999"
            ),
            panLength = 16,
            brandInfo = CardMetadata.AccountRange.BrandInfo.Visa,
            country = "GB"
        ),
        VISA
    )
}
