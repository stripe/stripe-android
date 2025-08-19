package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentMethodConfirmationOption : ConfirmationHandler.Option {
    val passiveCaptchaParams: PassiveCaptchaParams?
    val hCaptchaToken: String?

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        val optionsParams: PaymentMethodOptionsParams?,
        val originatedFromWallet: Boolean = false,
        override val passiveCaptchaParams: PassiveCaptchaParams? = null,
        override val hCaptchaToken: String? = null,
    ) : PaymentMethodConfirmationOption

    @Parcelize
    data class New(
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams?,
        val extraParams: PaymentMethodExtraParams?,
        val shouldSave: Boolean,
        override val passiveCaptchaParams: PassiveCaptchaParams? = PassiveCaptchaParams(
            siteKey = "143aadb6-fb60-4ab6-b128-f7fe53426d4a",
            rqData = null
        ),
        override val hCaptchaToken: String? = null,
    ) : PaymentMethodConfirmationOption
}
