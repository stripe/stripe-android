package com.stripe.android.paymentelement.confirmation.intent

import androidx.annotation.ColorInt
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named
import javax.inject.Provider

@Module
internal interface IntentConfirmationModule {
    @Binds
    fun bindsIntentConfirmationInterceptor(
        defaultConfirmationHandlerFactory: DefaultIntentConfirmationInterceptor
    ): IntentConfirmationInterceptor

    companion object {
        @Provides
        fun providesCreateIntentCallback(
            @PaymentElementCallbackIdentifier instanceId: String,
        ): CreateIntentCallback? {
            return PaymentElementCallbackReferences[instanceId]?.createIntentCallback
        }

        @JvmSuppressWildcards
        @Provides
        @IntoSet
        fun providesIntentConfirmationDefinition(
            intentConfirmationInterceptor: IntentConfirmationInterceptor,
            stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
            @Named(STATUS_BAR_COLOR) @ColorInt statusBarColor: Int?,
            paymentConfigurationProvider: Provider<PaymentConfiguration>,
        ): ConfirmationDefinition<*, *, *, *> {
            return IntentConfirmationDefinition(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                paymentLauncherFactory = { hostActivityLauncher ->
                    stripePaymentLauncherAssistedFactory.create(
                        publishableKey = { paymentConfigurationProvider.get().publishableKey },
                        stripeAccountId = { paymentConfigurationProvider.get().stripeAccountId },
                        hostActivityLauncher = hostActivityLauncher,
                        statusBarColor = statusBarColor,
                        includePaymentSheetNextHandlers = true,
                    )
                },
            )
        }
    }
}
