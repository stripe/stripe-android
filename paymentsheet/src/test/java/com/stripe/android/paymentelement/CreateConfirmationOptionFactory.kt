package com.stripe.android.paymentelement

import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.paymentelement.confirmation.CreateConfirmationOption
import com.stripe.android.paymentelement.confirmation.DefaultCreateConfirmationOption

internal fun createConfirmationOption(
    cardFundingFilterFactory: PaymentSheetCardFundingFilterFactory = FakeCardFundingFilterFactory()
): CreateConfirmationOption {
    return DefaultCreateConfirmationOption(cardFundingFilterFactory)
}
