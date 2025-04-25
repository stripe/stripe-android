package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

internal data class CustomPaymentMethodInput(
    val paymentElementCallbackIdentifier: String,
    val type: PaymentSheet.CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
)
