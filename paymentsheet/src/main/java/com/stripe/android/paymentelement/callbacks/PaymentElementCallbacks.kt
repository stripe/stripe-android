package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler

internal class PaymentElementCallbacks(
    val createIntentCallback: CreateIntentCallback?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
)
