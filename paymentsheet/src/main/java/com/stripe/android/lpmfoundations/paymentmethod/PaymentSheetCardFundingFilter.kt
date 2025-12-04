package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardFundingFilter
import com.stripe.android.model.CardFunding
import kotlinx.parcelize.Parcelize

@Parcelize
class PaymentSheetCardFundingFilter: CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        return true
    }
}
