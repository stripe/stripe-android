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
    private val userInput: UserInput,
) : ConfirmationHandler.Option {
    enum class PaymentMethodSaveOption(val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?) {
        RequestedReuse(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession),
        RequestedNoReuse(ConfirmPaymentIntentParams.SetupFutureUsage.Blank),
        NoRequest(null)
    }

    val sanitizedUserInput: UserInput
        get() = when (userInput) {
            is UserInput.SignIn -> {
                userInput
            }
            is UserInput.SignUp -> {
                val didSeeFullSignupForm = userInput.phone != null
                val phone = userInput.phone ?: createParams.billingDetails?.phone

                val country = if (didSeeFullSignupForm) {
                    userInput.country
                } else {
                    // The user saw the Link signup opt-in checkbox, so infer the country from the phone number
                    // or the billing details.
                    val billingPhoneCountry = (extraParams as? PaymentMethodExtraParams.Card)?.phoneNumberCountry
                    billingPhoneCountry ?: createParams.billingDetails?.address?.country
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
