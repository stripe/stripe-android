package com.stripe.android.ui.core.elements

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.CardFunding

interface CardFundingValidator {
    fun isValid(
        number: String,
        funding: CardFunding?
    ): Boolean
}

class DefaultCardFundingValidator(
    private val cardFundingFilter: CardFundingFilter
): CardFundingValidator {
    override fun isValid(number: String, funding: CardFunding?): Boolean {
        val cardNumber = CardNumber.Unvalidated(number)
        if (cardNumber.bin == null) return true
        return cardFundingFilter.isAccepted(funding)
    }
}
