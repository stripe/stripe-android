package com.stripe.android.paymentelement

import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.paymentsheet.PaymentSheet

internal class FakeCardFundingFilterFactory : PaymentSheetCardFundingFilterFactory {
    override fun invoke(params: List<PaymentSheet.CardFundingType>): CardFundingFilter {
        return DefaultCardFundingFilter
    }
}
