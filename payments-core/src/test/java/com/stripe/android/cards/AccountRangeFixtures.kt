package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange

internal object AccountRangeFixtures {
    val VISA = AccountRange(
        binRange = BinRange(
            low = "4242424240000000",
            high = "4242424249999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.Visa,
        country = "GB"
    )

    val AMERICANEXPRESS = AccountRange(
        binRange = BinRange(
            low = "378282000000000",
            high = "378282999999999"
        ),
        panLength = 15,
        brandInfo = AccountRange.BrandInfo.AmericanExpress,
        country = "US"
    )

    val MASTERCARD = AccountRange(
        binRange = BinRange(
            low = "5100000000000000",
            high = "5599999999999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.Mastercard
    )

    val JCB = AccountRange(
        binRange = BinRange(
            low = "3528000000000000",
            high = "3589999999999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.JCB
    )

    val DINERSCLUB14 = AccountRange(
        binRange = BinRange(
            low = "36000000000000",
            high = "36999999999999"
        ),
        panLength = 14,
        brandInfo = AccountRange.BrandInfo.DinersClub
    )

    val DINERSCLUB16 = AccountRange(
        binRange = BinRange(
            low = "3000000000000000",
            high = "3059999999999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.DinersClub
    )

    val DISCOVER = AccountRange(
        binRange = BinRange(
            low = "6011000000000000",
            high = "6011011999999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.Discover,
        country = "US"
    )

    val UNIONPAY19 = AccountRange(
        binRange = BinRange(
            low = "6216828050000000000",
            high = "6216828059999999999"
        ),
        panLength = 19,
        brandInfo = AccountRange.BrandInfo.UnionPay,
        country = "CN"
    )

    val UNIONPAY16 = AccountRange(
        binRange = BinRange(
            low = "3568400000000000",
            high = "3568409999999999"
        ),
        panLength = 16,
        brandInfo = AccountRange.BrandInfo.UnionPay,
        country = "CN"
    )

    val DEFAULT = listOf(
        AccountRange(
            binRange = BinRange(
                low = "4242420000000000",
                high = "4242424239999999"
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            country = "GB"
        ),
        VISA
    )
}
