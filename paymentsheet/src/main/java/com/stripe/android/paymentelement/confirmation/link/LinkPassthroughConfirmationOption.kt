package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkPassthroughConfirmationOption(
    val paymentDetailsId: String,
    val expectedPaymentMethodType: String,
    val cvc: String?,
    val billingPhone: String?,
) : ConfirmationHandler.Option
