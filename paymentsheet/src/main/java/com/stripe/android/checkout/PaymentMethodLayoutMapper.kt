package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentElement.PaymentMethodLayout.asPaymentSheet(): PaymentSheet.PaymentMethodLayout {
    return when (this) {
        PaymentElement.PaymentMethodLayout.Horizontal -> PaymentSheet.PaymentMethodLayout.Horizontal
        PaymentElement.PaymentMethodLayout.Vertical -> PaymentSheet.PaymentMethodLayout.Vertical
        PaymentElement.PaymentMethodLayout.Automatic -> PaymentSheet.PaymentMethodLayout.Automatic
    }
}
