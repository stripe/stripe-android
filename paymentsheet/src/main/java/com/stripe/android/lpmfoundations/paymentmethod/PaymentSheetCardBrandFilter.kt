package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentSheetCardBrandFilter(
    private val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
) : CardBrandFilter {

    override fun isAccepted(cardBrand: CardBrand): Boolean {
        val brandCategory = cardBrand.toBrandCategory()

        return when (cardBrandAcceptance) {
            is PaymentSheet.CardBrandAcceptance.All -> true

            is PaymentSheet.CardBrandAcceptance.Allowed -> {
                val isAllowed = brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)
                isAllowed
            }

            is PaymentSheet.CardBrandAcceptance.Disallowed -> {
                val isDisallowed = brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)
                !isDisallowed
            }
        }
    }

    fun isAccepted(paymentMethod: PaymentMethod): Boolean {
        val brand = paymentMethod.card?.displayBrand?.let { displayBrand ->
            val cardBrand = CardBrand.fromCode(displayBrand)
            if (cardBrand == CardBrand.Unknown) null else cardBrand
        } ?: paymentMethod.card?.brand ?: CardBrand.Unknown

        return paymentMethod.type != PaymentMethod.Type.Card || isAccepted(brand)
    }
}

// Extension function to map CardBrand to BrandCategory
internal fun CardBrand.toBrandCategory(): PaymentSheet.CardBrandAcceptance.BrandCategory? {
    return when (this) {
        CardBrand.Visa -> PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        CardBrand.MasterCard -> PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard
        CardBrand.AmericanExpress -> PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
        CardBrand.Discover,
        CardBrand.DinersClub,
        CardBrand.JCB,
        CardBrand.UnionPay -> PaymentSheet.CardBrandAcceptance.BrandCategory.Discover
        else -> null
    }
}
