package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class AccountRange internal constructor(
    val binRange: BinRange,
    val panLength: Int,
    val brandInfo: BrandInfo,
    val country: String? = null
) : StripeModel {
    val brand: CardBrand
        get() = brandInfo.brand

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class BrandInfo(
        val brandName: String,
        val brand: CardBrand
    ) {
        Visa("VISA", CardBrand.Visa),
        Mastercard("MASTERCARD", CardBrand.MasterCard),
        AmericanExpress("AMERICAN_EXPRESS", CardBrand.AmericanExpress),
        JCB("JCB", CardBrand.JCB),
        DinersClub("DINERS_CLUB", CardBrand.DinersClub),
        Discover("DISCOVER", CardBrand.Discover),
        UnionPay("UNIONPAY", CardBrand.UnionPay),
        CartesBancaires("CARTES_BANCAIRES", CardBrand.CartesBancaires),
    }
}
