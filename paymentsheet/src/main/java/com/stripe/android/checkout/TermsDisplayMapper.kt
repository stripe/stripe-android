package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun Map<PaymentMethod.Type, PaymentElement.Configuration.TermsDisplay>.asPaymentSheet():
    Map<PaymentMethod.Type, PaymentSheet.TermsDisplay> =
    mapValues { (_, termsDisplay) -> termsDisplay.asPaymentSheet() }

@OptIn(CheckoutSessionPreview::class)
private fun PaymentElement.Configuration.TermsDisplay.asPaymentSheet(): PaymentSheet.TermsDisplay = when (this) {
    PaymentElement.Configuration.TermsDisplay.AUTOMATIC -> PaymentSheet.TermsDisplay.AUTOMATIC
    PaymentElement.Configuration.TermsDisplay.NEVER -> PaymentSheet.TermsDisplay.NEVER
}
