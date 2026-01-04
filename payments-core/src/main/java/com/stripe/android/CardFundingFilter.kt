package com.stripe.android

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardFunding
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardFundingFilter : Parcelable {
    fun isAccepted(cardFunding: CardFunding): Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Factory<T> {
        operator fun invoke(params: T): CardFundingFilter
    }
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultCardFundingFilter : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        return true
    }
}
