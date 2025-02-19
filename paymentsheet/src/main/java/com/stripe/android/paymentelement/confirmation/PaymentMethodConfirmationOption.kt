package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentMethodConfirmationOption : ConfirmationHandler.Option {
    @Parcelize
    data class Saved(
        val paymentMethod: com.stripe.android.model.PaymentMethod,
        val optionsParams: PaymentMethodOptionsParams?,
    ) : PaymentMethodConfirmationOption

    @Parcelize
    data class New(
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams?,
        val extraParams: PaymentMethodExtraParams?,
        val shouldSave: Boolean
    ) : PaymentMethodConfirmationOption
}
