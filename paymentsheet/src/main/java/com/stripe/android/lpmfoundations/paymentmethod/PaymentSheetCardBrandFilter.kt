package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.CardBrandFilter
import kotlinx.android.parcel.Parcelize

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
                if (brandCategory != null && !cardBrandAcceptance.brands.contains(brandCategory)) {
                    // Log event if necessary
                    // For example: Log.d("PaymentSheet", "${cardBrand.name} is not accepted")
                    false
                } else {
                    true
                }
            }
            is PaymentSheet.CardBrandAcceptance.Disallowed -> {
                val brandCategory = cardBrand.toBrandCategory()
                if (brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)) {
                    // Log event if necessary
                    // For example: Log.d("PaymentSheet", "${cardBrand.name} is not accepted")
                    false
                } else {
                    true
                }
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
        // Handle other brands as needed
        else -> null
    }
}
