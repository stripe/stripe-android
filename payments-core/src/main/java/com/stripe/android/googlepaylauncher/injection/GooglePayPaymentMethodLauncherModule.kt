package com.stripe.android.googlepaylauncher.injection

import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.PaymentsClientFactory
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

    companion object {
        @Provides
        @Singleton
        fun providePaymentsClient(
            googlePayConfig: GooglePayPaymentMethodLauncher.Config,
            paymentsClientFactory: PaymentsClientFactory
        ) = paymentsClientFactory.create(googlePayConfig.environment)
    }
}
