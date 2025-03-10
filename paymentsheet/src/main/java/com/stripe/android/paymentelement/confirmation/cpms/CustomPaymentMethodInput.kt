package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal data class CustomPaymentMethodInput(
    val type: PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType,
    val billingDetails: PaymentMethod.BillingDetails?,
)
