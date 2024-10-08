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
        return when (cardBrandAcceptance) {
            is PaymentSheet.CardBrandAcceptance.All -> {
                true
            }
            is PaymentSheet.CardBrandAcceptance.Allowed -> {
                val brandCategory = cardBrand.toBrandCategory()
                // If a merchant has specified brands to allow, block unknown brands
                if (brandCategory == null) {
                    // TODO(porter) Log event for disallowed brand
                    false
                } else if (!cardBrandAcceptance.brands.contains(brandCategory)) {
                    // TODO(porter) Log event for disallowed brand
                    false
                } else {
                    true
                }
            }
            is PaymentSheet.CardBrandAcceptance.Disallowed -> {
                val brandCategory = cardBrand.toBrandCategory()
                if (brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)) {
                    // TODO(porter) Log event for disallowed brand
                    false
                } else {
                    true
                }
            }
        }
    }

    companion object {
        val DEFAULT = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.All)
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
