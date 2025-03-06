package com.stripe.android.paymentelement.confirmation.linkinline

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkInlineSignupConfirmationOption(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams?,
    val extraParams: PaymentMethodExtraParams?,
    val saveOption: PaymentMethodSaveOption,
    val linkConfiguration: LinkConfiguration,
    val userInput: UserInput,
) : ConfirmationHandler.Option {
    enum class PaymentMethodSaveOption(val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?) {
        RequestedReuse(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession),
        RequestedNoReuse(ConfirmPaymentIntentParams.SetupFutureUsage.Blank),
        NoRequest(null)
    }
}
