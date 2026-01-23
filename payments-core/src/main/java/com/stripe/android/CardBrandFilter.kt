package com.stripe.android

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardBrandFilter : Parcelable {
    fun isAccepted(cardBrand: CardBrand): Boolean
    fun isAccepted(paymentMethod: PaymentMethod): Boolean
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultCardBrandFilter : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return true
    }

    override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
        return true
    }
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class AcceptanceCardBrandFilter(
    private val cardBrandAcceptance: CardBrand.CardBrandAcceptance
) : CardBrandFilter {

    override fun isAccepted(cardBrand: CardBrand): Boolean {
        val brandCategory = cardBrand.toBrandCategory()

        return when (cardBrandAcceptance) {
            is CardBrand.CardBrandAcceptance.All -> true

            is CardBrand.CardBrandAcceptance.Allowed -> {
                val isAllowed = brandCategory != null && cardBrandAcceptance.brands.contains(brandCategory)
                isAllowed
            }

            is CardBrand.CardBrandAcceptance.Disallowed -> {
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
