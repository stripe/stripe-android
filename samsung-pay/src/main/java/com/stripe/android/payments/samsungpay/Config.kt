package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Config @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    val serviceId: String,
    val merchantId: String,
    val merchantName: String,
    val cardBrandFilter: CardBrandFilter,
) : Parcelable {

    constructor(
        serviceId: String,
        merchantId: String,
        merchantName: String,
        allowedCardBrands: List<CardBrand> = listOf(
            CardBrand.Visa,
            CardBrand.MasterCard,
            CardBrand.AmericanExpress,
            CardBrand.Discover,
        ),
    ) : this(
        serviceId = serviceId,
        merchantId = merchantId,
        merchantName = merchantName,
        cardBrandFilter = AllowListCardBrandFilter(allowedCardBrands.toSet()),
    )
}

@Parcelize
private class AllowListCardBrandFilter(
    private val allowed: Set<CardBrand>,
) : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean = cardBrand in allowed
    override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
        val brand = paymentMethod.card?.brand ?: return true
        return brand in allowed
    }
}
