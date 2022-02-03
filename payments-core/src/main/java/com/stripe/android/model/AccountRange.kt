package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AccountRange internal constructor(
    val binRange: BinRange,
    val panLength: Int,
    val brandInfo: BrandInfo,
    val country: String? = null
) : StripeModel {
    val brand: com.stripe.android.ui.core.elements.CardBrand
        get() = brandInfo.brand

    internal enum class BrandInfo(
        val brandName: String,
        val brand: com.stripe.android.ui.core.elements.CardBrand
    ) {
        Visa("VISA", com.stripe.android.ui.core.elements.CardBrand.Visa),
        Mastercard("MASTERCARD", com.stripe.android.ui.core.elements.CardBrand.MasterCard),
        AmericanExpress("AMERICAN_EXPRESS", com.stripe.android.ui.core.elements.CardBrand.AmericanExpress),
        JCB("JCB", com.stripe.android.ui.core.elements.CardBrand.JCB),
        DinersClub("DINERS_CLUB", com.stripe.android.ui.core.elements.CardBrand.DinersClub),
        Discover("DISCOVER", com.stripe.android.ui.core.elements.CardBrand.Discover),
        UnionPay("UNIONPAY", com.stripe.android.ui.core.elements.CardBrand.UnionPay)
    }
}
