package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentMethodConfirmationOption : ConfirmationHandler.Option {
    val passiveCaptchaParams: PassiveCaptchaParams?
    val optionsParams: PaymentMethodOptionsParams?

    @Parcelize
    data class Saved(
        val paymentMethod: com.stripe.android.model.PaymentMethod,
        override val optionsParams: PaymentMethodOptionsParams?,
        val originatedFromWallet: Boolean = false,
        override val passiveCaptchaParams: PassiveCaptchaParams?,
        val hCaptchaToken: String? = null,
    ) : PaymentMethodConfirmationOption

    @Parcelize
    data class New(
        val createParams: PaymentMethodCreateParams,
        override val optionsParams: PaymentMethodOptionsParams?,
        val extraParams: PaymentMethodExtraParams?,
        val shouldSave: Boolean,
        override val passiveCaptchaParams: PassiveCaptchaParams?,
    ) : PaymentMethodConfirmationOption
}
