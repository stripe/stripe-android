package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Identifiable

internal data class CustomPaymentMethodInput(
    val paymentElementCallbackIdentifier: Identifiable,
    val type: PaymentSheet.CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
)
