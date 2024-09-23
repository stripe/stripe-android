package com.stripe.android

import com.stripe.android.model.CardBrand
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface CardBrandFilter : Parcelable {
    fun isAccepted(cardBrand: CardBrand): Boolean
}

@Parcelize
class DefaultCardBrandFilter : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return true
    }
}