package com.stripe.android.utils

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@Parcelize
class FakeCardBrandFilter(
    private val disallowedBrands: Set<CardBrand>
) : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return !disallowedBrands.contains(cardBrand)
    }

    override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
        throw NotImplementedError()
    }
}
