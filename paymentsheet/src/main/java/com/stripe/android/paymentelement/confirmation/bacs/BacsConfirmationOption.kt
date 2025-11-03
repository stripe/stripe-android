package com.stripe.android.paymentelement.confirmation.bacs

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BacsConfirmationOption(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams?,
) : ConfirmationHandler.Option
