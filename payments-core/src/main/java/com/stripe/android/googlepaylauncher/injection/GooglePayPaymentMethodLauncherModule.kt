package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.PaymentsClientFactory
import com.stripe.android.googlepaylauncher.convert
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module(
    subcomponents = [GooglePayPaymentMethodLauncherViewModelSubcomponent::class]
)
internal class GooglePayPaymentMethodLauncherModule {
    /**
     * Because [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID] might change, each time a new [ApiRequest]
     * is to be send through [StripeRepository], a new [ApiRequest.Options]
     * instance is created with the latest values.
     *
     * Should always be used with [Provider] or [Lazy].
     */
    @Provides
    fun provideApiRequestOptions(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?
    ) = ApiRequest.Options(
        apiKey = publishableKeyProvider(),
        stripeAccount = stripeAccountIdProvider()
    )

    @Provides
    @Singleton
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    @Singleton
    fun provideGooglePayRepository(
        context: Context,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config,
        logger: Logger
    ): GooglePayRepository = DefaultGooglePayRepository(
        context.applicationContext,
        googlePayConfig.environment,
        googlePayConfig.billingAddressConfig.convert(),
        googlePayConfig.existingPaymentMethodRequired,
        logger
    )

    @Provides
    @Singleton
    fun provideGooglePayJsonFactory(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config
    ) = GooglePayJsonFactory(
        googlePayConfig = GooglePayConfig(publishableKeyProvider(), stripeAccountIdProvider()),
        isJcbEnabled = googlePayConfig.isJcbEnabled
    )

    @Provides
    @Singleton
    fun providePaymentsClient(
        context: Context,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config
    ) = PaymentsClientFactory(context).create(googlePayConfig.environment)
}
