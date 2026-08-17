package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview as PaymentSheetCardFundingFilteringPrivatePreview

@OptIn(
    CheckoutSessionPreview::class,
    CardFundingFilteringPrivatePreview::class,
    PaymentSheetCardFundingFilteringPrivatePreview::class,
)
internal fun List<PaymentElement.Configuration.CardFundingType>.asPaymentSheet(): List<PaymentSheet.CardFundingType> =
    map { cardFundingType ->
        when (cardFundingType) {
            PaymentElement.Configuration.CardFundingType.Debit -> PaymentSheet.CardFundingType.Debit
            PaymentElement.Configuration.CardFundingType.Credit -> PaymentSheet.CardFundingType.Credit
            PaymentElement.Configuration.CardFundingType.Prepaid -> PaymentSheet.CardFundingType.Prepaid
            PaymentElement.Configuration.CardFundingType.Unknown -> PaymentSheet.CardFundingType.Unknown
        }
    }
