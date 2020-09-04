package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class AccountRange internal constructor(
    val binRange: BinRange,
    val panLength: Int,
    val brandInfo: BrandInfo,
    val country: String? = null
) : StripeModel {
    val brand: CardBrand
        get() = brandInfo.brand

    internal enum class BrandInfo(
        val brandName: String,
        val brand: CardBrand
    ) {
        Visa("VISA", CardBrand.Visa),
        Mastercard("MASTERCARD", CardBrand.MasterCard),
        AmericanExpress("AMERICAN_EXPRESS", CardBrand.AmericanExpress),
        JCB("JCB", CardBrand.JCB),
        DinersClub("DINERS_CLUB", CardBrand.DinersClub),
        Discover("DISCOVER", CardBrand.Discover),
        UnionPay("UNIONPAY", CardBrand.UnionPay)
    }
}
