package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.utils.updatedWithPmoSfu
import com.stripe.android.paymentelement.confirmation.utils.updatedWithProductUsage
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentMethodConfirmationOption : ConfirmationHandler.Option {
    val passiveCaptchaParams: PassiveCaptchaParams?
    val optionsParams: PaymentMethodOptionsParams?
    val clientAttributionMetadata: ClientAttributionMetadata?

    fun updatedForDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
    ): PaymentMethodConfirmationOption

    @Parcelize
    data class Saved(
        val paymentMethod: com.stripe.android.model.PaymentMethod,
        override val optionsParams: PaymentMethodOptionsParams?,
        override val clientAttributionMetadata: ClientAttributionMetadata? = null,
        val originatedFromWallet: Boolean = false,
        override val passiveCaptchaParams: PassiveCaptchaParams?,
        val hCaptchaToken: String? = null,
    ) : PaymentMethodConfirmationOption {
        override fun updatedForDeferredIntent(
            intentConfiguration: PaymentSheet.IntentConfiguration,
        ): Saved {
            val updatedOptionsParams = optionsParams.updatedWithPmoSfu(
                code = paymentMethod.type?.code,
                intentConfiguration = intentConfiguration,
            )
            return copy(
                optionsParams = updatedOptionsParams,
            )
        }
    }

    @Parcelize
    data class New(
        val createParams: PaymentMethodCreateParams,
        override val optionsParams: PaymentMethodOptionsParams?,
        val extraParams: PaymentMethodExtraParams?,
        val shouldSave: Boolean,
        override val passiveCaptchaParams: PassiveCaptchaParams?,
    ) : PaymentMethodConfirmationOption {

        override val clientAttributionMetadata: ClientAttributionMetadata?
            get() = null

        override fun updatedForDeferredIntent(
            intentConfiguration: PaymentSheet.IntentConfiguration,
        ): New {
            val updatedCreateParams = createParams.updatedWithProductUsage(intentConfiguration)
            val updatedOptionsParams = optionsParams.updatedWithPmoSfu(
                code = updatedCreateParams.typeCode,
                intentConfiguration = intentConfiguration,
            )
            return copy(
                createParams = updatedCreateParams,
                optionsParams = updatedOptionsParams,
            )
        }
    }
}
