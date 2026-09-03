package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentElement.Configuration.PaymentMethodLayout.asPaymentSheet(): PaymentSheet.PaymentMethodLayout {
    return when (this) {
        PaymentElement.Configuration.PaymentMethodLayout.Horizontal -> PaymentSheet.PaymentMethodLayout.Horizontal
        PaymentElement.Configuration.PaymentMethodLayout.Vertical -> PaymentSheet.PaymentMethodLayout.Vertical
        PaymentElement.Configuration.PaymentMethodLayout.PreferForm -> PaymentSheet.PaymentMethodLayout.Vertical
        PaymentElement.Configuration.PaymentMethodLayout.Automatic -> PaymentSheet.PaymentMethodLayout.Automatic
    }
}
