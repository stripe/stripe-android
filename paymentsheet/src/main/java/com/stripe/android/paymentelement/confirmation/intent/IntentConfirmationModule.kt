package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal class IntentConfirmationModule {
    @Provides
    fun providesCreateIntentCallback(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
    ): CreateIntentCallback? {
        return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.createIntentCallback
    }

    @Provides
    fun providesCreateIntentWithConfirmationTokenCallback(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
    ): CreateIntentWithConfirmationTokenCallback? {
        return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
            ?.createIntentWithConfirmationTokenCallback
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Provides
    fun providesPreparePaymentMethodHandler(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
    ): PreparePaymentMethodHandler? {
        return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.preparePaymentMethodHandler
    }

    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesIntentConfirmationDefinition(
        interceptorFactory: IntentConfirmationInterceptor.Factory,
        stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
    ): ConfirmationDefinition<*, *, *, *> {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptorFactory = interceptorFactory,
            paymentLauncherFactory = { hostActivityLauncher, statusBarColor, apiConfig ->
                stripePaymentLauncherAssistedFactory.create(
                    publishableKey = { apiConfig.publishableKey },
                    stripeAccountId = { apiConfig.stripeAccountId },
                    hostActivityLauncher = hostActivityLauncher,
                    statusBarColor = statusBarColor,
                    includePaymentSheetNextHandlers = true,
                )
            },
        )
    }
}
