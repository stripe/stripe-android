package com.stripe.android.testing

import com.stripe.android.CardFundingFilter
import com.stripe.android.model.CardFunding
import kotlinx.parcelize.Parcelize

@Parcelize
class FakeCardFundingFilter(
    private val disallowedFundingTypes: Set<CardFunding> = emptySet(),
    private val messageResId: Int? = null
) : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        return !disallowedFundingTypes.contains(cardFunding)
    }

    override fun allowedFundingTypesDisplayMessage(): Int? {
        return messageResId
    }
}
