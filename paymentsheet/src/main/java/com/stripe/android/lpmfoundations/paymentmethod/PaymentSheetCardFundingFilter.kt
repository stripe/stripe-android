package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardFundingFilter
import com.stripe.android.model.CardFunding
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import com.stripe.android.R as StripeR

typealias PaymentSheetCardFundingFilterFactory = CardFundingFilter.Factory<List<PaymentSheet.CardFundingType>>

@Parcelize
internal data class PaymentSheetCardFundingFilter(
    private val allowedCardFundingTypes: List<PaymentSheet.CardFundingType>
) : CardFundingFilter {
    override fun isAccepted(cardFunding: CardFunding): Boolean {
        if (cardFunding == CardFunding.Unknown) return true
        return allowedCardFundingTypes.any { it.cardFunding == cardFunding }
    }

    override fun allowedFundingTypesDisplayMessage(): Int? {
        val acceptedFundingTypes = allowedCardFundingTypes.map { it.cardFunding }.toSet()
        val credit = acceptedFundingTypes.contains(CardFunding.Credit)
        val debit = acceptedFundingTypes.contains(CardFunding.Debit)
        val prepaid = acceptedFundingTypes.contains(CardFunding.Prepaid)

        return when {
            credit && debit && prepaid -> {
                null
            }
            credit && debit -> StripeR.string.stripe_card_funding_only_debit_credit
            credit && prepaid -> StripeR.string.stripe_card_funding_only_credit_prepaid
            debit && prepaid -> StripeR.string.stripe_card_funding_only_debit_prepaid
            credit -> StripeR.string.stripe_card_funding_only_credit
            debit -> StripeR.string.stripe_card_funding_only_debit
            prepaid -> StripeR.string.stripe_card_funding_only_prepaid
            else -> {
                null
            }
        }
    }

    class Factory @Inject constructor() : PaymentSheetCardFundingFilterFactory {
        override fun invoke(params: List<PaymentSheet.CardFundingType>): CardFundingFilter {
            return PaymentSheetCardFundingFilter(params)
        }
    }
}
