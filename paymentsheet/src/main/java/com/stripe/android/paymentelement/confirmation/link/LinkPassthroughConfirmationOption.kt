package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkPassthroughConfirmationOption(
    val paymentDetailsId: String,
    val expectedPaymentMethodType: String,
    val optionsParams: PaymentMethodOptionsParams,
) : ConfirmationHandler.Option
