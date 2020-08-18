package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class CardMetadata internal constructor(
    val binPrefix: String,
    val accountRanges: List<AccountRange>
) : StripeModel {

    @Parcelize
    internal data class AccountRange internal constructor(
        val binRange: BinRange,
        val panLength: Int,
        val brandName: BrandName,
        val country: String? = null
    ) : StripeModel {
        val brand: CardBrand
            get() = brandName.brand

        internal enum class BrandName(
            val code: String,
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
}
