package com.stripe.android

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardFunding
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardFundingFilter: Parcelable {
    fun isAccepted(cardFunding: CardFunding): Boolean
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultCardFundingFilter : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        return true
    }
}
