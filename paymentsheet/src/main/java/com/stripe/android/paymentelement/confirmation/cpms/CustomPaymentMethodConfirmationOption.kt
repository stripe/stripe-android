package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@Parcelize
data class CustomPaymentMethodConfirmationOption(
    val customPaymentMethodType: PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType,
    val billingDetails: PaymentMethod.BillingDetails?,
) : ConfirmationHandler.Option
