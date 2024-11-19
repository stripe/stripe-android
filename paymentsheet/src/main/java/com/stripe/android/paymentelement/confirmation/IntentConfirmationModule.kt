package com.stripe.android.paymentelement.confirmation

import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
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
        @JvmSuppressWildcards
        @Provides
        @IntoSet
        fun providesIntentConfirmationDefinition(
            intentConfirmationInterceptor: IntentConfirmationInterceptor,
            stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
            @Named(STATUS_BAR_COLOR_PROVIDER) statusBarColor: () -> Int?,
            paymentConfigurationProvider: Provider<PaymentConfiguration>,
        ): ConfirmationDefinition<*, *, *, *> {
            return IntentConfirmationDefinition(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                paymentLauncherFactory = { hostActivityLauncher ->
                    stripePaymentLauncherAssistedFactory.create(
                        publishableKey = { paymentConfigurationProvider.get().publishableKey },
                        stripeAccountId = { paymentConfigurationProvider.get().stripeAccountId },
                        hostActivityLauncher = hostActivityLauncher,
                        statusBarColor = statusBarColor(),
                        includePaymentSheetNextHandlers = true,
                    )
                },
            )
        }
    }
}
