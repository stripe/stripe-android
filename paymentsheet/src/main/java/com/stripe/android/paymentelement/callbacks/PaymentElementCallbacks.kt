package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentelement.CustomPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class PaymentElementCallbacks(
    val createIntentCallback: CreateIntentCallback?,
    val customPaymentMethodConfirmHandler: CustomPaymentMethodConfirmHandler?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
)
