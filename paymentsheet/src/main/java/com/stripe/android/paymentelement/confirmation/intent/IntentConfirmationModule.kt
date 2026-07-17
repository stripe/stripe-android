package com.stripe.android.paymentelement.confirmation.intent

import androidx.annotation.ColorInt
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named

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
        @Named(STATUS_BAR_COLOR) @ColorInt statusBarColor: Int?,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
    ): ConfirmationDefinition<*, *, *, *> {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptorFactory = interceptorFactory,
            paymentLauncherFactory = { hostActivityLauncher ->
                stripePaymentLauncherAssistedFactory.create(
                    publishableKey = publishableKeyProvider,
                    stripeAccountId = stripeAccountIdProvider,
                    hostActivityLauncher = hostActivityLauncher,
                    statusBarColor = statusBarColor,
                    includePaymentSheetNextHandlers = true,
                )
            },
        )
    }
}
