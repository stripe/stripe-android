package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler.Option
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ExternalPaymentMethodConfirmationOption(
    val type: String,
    val billingDetails: PaymentMethod.BillingDetails?,
) : Option
