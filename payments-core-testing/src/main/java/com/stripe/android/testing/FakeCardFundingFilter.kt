package com.stripe.android.testing

import com.stripe.android.CardFundingFilter
import com.stripe.android.model.CardFunding
import kotlinx.parcelize.Parcelize

@Parcelize
class FakeCardFundingFilter(
    private val isAccepted: () -> Boolean
) : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding?): Boolean = isAccepted()
}
