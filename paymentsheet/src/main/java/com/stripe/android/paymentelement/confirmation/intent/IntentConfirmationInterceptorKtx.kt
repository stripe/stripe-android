package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal suspend fun IntentConfirmationInterceptor.intercept(
    confirmationOption: PaymentMethodConfirmationOption,
    intent: StripeIntent,
    initializationMode: PaymentElementLoader.InitializationMode,
    shippingDetails: AddressDetails?
): ConfirmationDefinition.Action<Args> {
    return when (confirmationOption) {
        is PaymentMethodConfirmationOption.New -> {
            intercept(
                initializationMode = initializationMode,
                intent = intent,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodCreateParams = confirmationOption.createParams,
                paymentMethodExtraParams = confirmationOption.extraParams,
                shippingValues = shippingDetails?.toConfirmPaymentIntentShipping(),
                customerRequestedSave = confirmationOption.shouldSave,
            )
        }
        is PaymentMethodConfirmationOption.Saved -> {
            intercept(
                initializationMode = initializationMode,
                intent = intent,
                paymentMethod = confirmationOption.paymentMethod,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodExtraParams = null,
                shippingValues = shippingDetails?.toConfirmPaymentIntentShipping(),
                hCaptchaToken = confirmationOption.hCaptchaToken
            )
        }
    }
}
