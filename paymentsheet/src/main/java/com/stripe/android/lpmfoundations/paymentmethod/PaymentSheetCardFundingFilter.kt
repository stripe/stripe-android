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
        return allowedCardFundingTypes.any { it.matches(cardFunding) }
    }

    private fun PaymentSheet.CardFundingType.matches(cardFunding: CardFunding): Boolean {
        return when (this) {
            PaymentSheet.CardFundingType.Debit -> {
                cardFunding == CardFunding.Debit
            }
            PaymentSheet.CardFundingType.Credit -> {
                cardFunding == CardFunding.Credit
            }
            PaymentSheet.CardFundingType.Prepaid -> {
                cardFunding == CardFunding.Prepaid
            }
            PaymentSheet.CardFundingType.Unknown -> {
                cardFunding == CardFunding.Unknown
            }
        }
    }
}
