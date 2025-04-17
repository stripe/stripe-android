package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomPaymentMethodConfirmationOption(
    val customPaymentMethodType: PaymentSheet.CustomPaymentMethod,
    val billingDetails: PaymentMethod.BillingDetails?,
) : ConfirmationHandler.Option
