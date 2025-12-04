package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardFundingFilter
import com.stripe.android.model.CardFunding
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentSheetCardFundingFilter(
    private val allowedCardFundingTypes: List<PaymentSheet.CardFundingType>
) : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        return allowedCardFundingTypes.any { it.cardFunding == cardFunding }
    }
}
