package com.stripe.android.googlepaylauncher.injection

import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.DefaultPaymentsClientFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.PaymentsClientFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [GooglePayPaymentMethodLauncherViewModelSubcomponent::class]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class GooglePayPaymentMethodLauncherModule {
    @Binds
    @Singleton
    abstract fun bindsGooglePayRepository(
        defaultGooglePayRepository: DefaultGooglePayRepository
    ): GooglePayRepository

    @Binds
    @Singleton
    abstract fun bindsPaymentsClientFactory(
        defaultPaymentsClientFactory: DefaultPaymentsClientFactory
    ): PaymentsClientFactory

    @Binds
    @Singleton
    abstract fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    ): AnalyticsRequestFactory

    @Binds
    @Singleton
    abstract fun bindsErrorReporter(
        realErrorReporter: RealErrorReporter
    ): ErrorReporter

    companion object {
        @Provides
        @Singleton
        fun providePaymentsClient(
            googlePayConfig: GooglePayPaymentMethodLauncher.Config,
            paymentsClientFactory: PaymentsClientFactory
        ) = paymentsClientFactory.create(googlePayConfig.environment)
    }
}
