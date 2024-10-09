@file:OptIn(ExperimentalCardBrandFilteringApi::class)

package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.CardBrandFilter
import com.stripe.android.ExperimentalCardBrandFilteringApi
import kotlinx.parcelize.Parcelize

@Parcelize
class PaymentSheetCardBrandFilter(
    private val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
) : CardBrandFilter {

    override fun isAccepted(cardBrand: CardBrand): Boolean {
        val brandCategory = cardBrand.toBrandCategory()

        return when (cardBrandAcceptance) {
            is PaymentSheet.CardBrandAcceptance.All -> true

            is PaymentSheet.CardBrandAcceptance.Allowed -> {
                val isAllowed = brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)
                if (!isAllowed) {
                    // TODO(porter) Log event for disallowed brand
                }
                isAllowed
            }

            is PaymentSheet.CardBrandAcceptance.Disallowed -> {
                val isDisallowed = brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)
                if (isDisallowed) {
                    // TODO(porter) Log event for disallowed brand
                }
                !isDisallowed
            }
        }
    }

}

// Extension function to map CardBrand to BrandCategory
fun CardBrand.toBrandCategory(): PaymentSheet.CardBrandAcceptance.BrandCategory? {
    return when (this) {
        CardBrand.Visa -> PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        CardBrand.MasterCard -> PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard
        CardBrand.AmericanExpress -> PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
        CardBrand.Discover,
        CardBrand.DinersClub,
        CardBrand.JCB,
        CardBrand.UnionPay -> PaymentSheet.CardBrandAcceptance.BrandCategory.DiscoverGlobalNetwork
        else -> null
    }
}
