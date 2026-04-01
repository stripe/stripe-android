package com.stripe.android.paymentelement.confirmation.linkinline

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

internal sealed interface LinkInlineSignupConfirmationOption : ConfirmationHandler.Option {
    val linkConfiguration: LinkConfiguration
    val sanitizedUserInput: UserInput

    @Parcelize
    data class New(
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams?,
        val extraParams: PaymentMethodExtraParams?,
        val saveOption: PaymentMethodSaveOption,
        override val linkConfiguration: LinkConfiguration,
        private val userInput: UserInput,
    ) : LinkInlineSignupConfirmationOption {
        override val sanitizedUserInput: UserInput
            get() = sanitizedUserInput(
                userInput = userInput,
                extraParams = extraParams,
                billingDetails = createParams.billingDetails,
            )
    }

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        val optionsParams: PaymentMethodOptionsParams?,
        override val linkConfiguration: LinkConfiguration,
        private val userInput: UserInput,
    ) : LinkInlineSignupConfirmationOption {
        override val sanitizedUserInput: UserInput
            get() = sanitizedUserInput(
                userInput = userInput,
                extraParams = null,
                billingDetails = paymentMethod.billingDetails,
            )
    }

    enum class PaymentMethodSaveOption(val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?) {
        RequestedReuse(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession),
        RequestedNoReuse(ConfirmPaymentIntentParams.SetupFutureUsage.Blank),
        NoRequest(null)
    }
}

private fun sanitizedUserInput(
    userInput: UserInput,
    extraParams: PaymentMethodExtraParams?,
    billingDetails: PaymentMethod.BillingDetails?,
): UserInput {
    return when (userInput) {
        is UserInput.SignIn -> {
            userInput
        }
        is UserInput.SignUp -> {
            val didSeeFullSignupForm = userInput.phone != null
            val phone = userInput.phone ?: billingDetails?.phone

            val country = if (didSeeFullSignupForm) {
                userInput.country
            } else {
                // The user saw the Link signup opt-in checkbox, so infer the country from the phone number
                // or the billing details.
                val billingPhoneCountry = (extraParams as? PaymentMethodExtraParams.Card)?.phoneNumberCountry
                billingPhoneCountry ?: billingDetails?.address?.country
            }

            val countryInferringMethod = if (phone != null) "PHONE_NUMBER" else "BILLING_ADDRESS"

            userInput.copy(
                phone = phone,
                country = country,
                countryInferringMethod = countryInferringMethod,
            )
        }
    }
}
