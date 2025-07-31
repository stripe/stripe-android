package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.elements.payment.CustomPaymentMethod
import com.stripe.android.model.PaymentMethod

internal data class CustomPaymentMethodInput(
    val paymentElementCallbackIdentifier: String,
    val type: CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
)
