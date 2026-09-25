package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.Identifiable
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal class IntentConfirmationModule {
    @Provides
    fun providesCreateIntentCallback(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: Identifiable,
    ): CreateIntentCallback? {
        return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.createIntentCallback
    }

    @Provides
    fun providesCreateIntentWithConfirmationTokenCallback(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: Identifiable,
    ): CreateIntentWithConfirmationTokenCallback? {
        return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
            ?.createIntentWithConfirmationTokenCallback
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Provides
    fun providesPreparePaymentMethodHandler(
        @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: Identifiable,
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
            paymentLauncherFactory = { hostActivityLauncher, statusBarColor, apiConfiguration ->
                stripePaymentLauncherAssistedFactory.create(
                    apiConfigurationProvider = { apiConfiguration },
                    hostActivityLauncher = hostActivityLauncher,
                    statusBarColor = statusBarColor,
                    includePaymentSheetNextHandlers = true,
                )
            },
        )
    }
}
