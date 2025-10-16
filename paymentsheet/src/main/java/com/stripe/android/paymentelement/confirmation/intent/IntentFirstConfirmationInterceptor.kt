package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.utils.ConfirmActionHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class IntentFirstConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val clientSecret: String,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {
    private val confirmActionHelper: ConfirmActionHelper = ConfirmActionHelper(requestOptions.apiKeyIsLiveMode)

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        return confirmActionHelper.createConfirmAction(
            clientSecret = clientSecret,
            intent = intent,
            shippingValues = shippingValues,
            isDeferred = false
        ) {
            create(
                confirmationOption.createParams,
                confirmationOption.optionsParams,
                confirmationOption.extraParams,
                clientAttributionMetadata = clientAttributionMetadata,
            )
        }
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        return confirmActionHelper.createConfirmAction(
            clientSecret = clientSecret,
            intent = intent,
            shippingValues = shippingValues,
            isDeferred = false,
        ) {
            create(
                paymentMethod = confirmationOption.paymentMethod,
                optionsParams = confirmationOption.optionsParams,
                extraParams = null,
                intentConfigSetupFutureUsage = null,
                radarOptions = confirmationOption.hCaptchaToken?.let { RadarOptions(it) },
                clientAttributionMetadata = clientAttributionMetadata,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            clientSecret: String,
            clientAttributionMetadata: ClientAttributionMetadata?,
        ): IntentFirstConfirmationInterceptor
    }
}
