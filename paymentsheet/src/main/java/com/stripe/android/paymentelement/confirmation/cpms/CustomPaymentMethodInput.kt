package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.Identifiable
import com.stripe.android.paymentsheet.PaymentSheet

internal data class CustomPaymentMethodInput(
    val paymentElementCallbackIdentifier: Identifiable,
    val type: PaymentSheet.CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
)
