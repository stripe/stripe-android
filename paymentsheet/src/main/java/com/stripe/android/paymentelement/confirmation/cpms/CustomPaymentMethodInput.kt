package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.callbacks.CallbacksKey
import com.stripe.android.paymentsheet.PaymentSheet

internal data class CustomPaymentMethodInput(
    val paymentElementCallbackIdentifier: CallbacksKey,
    val type: PaymentSheet.CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
)
