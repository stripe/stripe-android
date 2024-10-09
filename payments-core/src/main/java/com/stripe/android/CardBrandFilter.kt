package com.stripe.android

import com.stripe.android.model.CardBrand
import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardBrandFilter : Parcelable {
    fun isAccepted(cardBrand: CardBrand): Boolean
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardBrandFilter : CardBrandFilter {
    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return true
    }
}