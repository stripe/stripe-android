package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkCardBrandConfirmationOption(
    val paymentDetailsId: String,
    val expectedPaymentMethodType: String,
) : ConfirmationHandler.Option
