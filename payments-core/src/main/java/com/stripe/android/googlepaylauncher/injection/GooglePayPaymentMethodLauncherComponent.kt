package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Component that holds the dependency graph for the lifecycle of [GooglePayPaymentMethodLauncher]
 * and related classes.
 */
@Singleton
@Component(
    modules = [
        GooglePayPaymentMethodLauncherModule::class,
        LoggingModule::class
    ]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class GooglePayPaymentMethodLauncherComponent {
    abstract fun inject(factory: GooglePayPaymentMethodLauncherViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun ioContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun analyticsRequestFactory(paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory): Builder

        @BindsInstance
        fun stripeRepository(stripeRepository: StripeRepository): Builder

        @BindsInstance
        fun googlePayConfig(config: GooglePayPaymentMethodLauncher.Config): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        fun build(): GooglePayPaymentMethodLauncherComponent
    }
}
