package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Component that holds the dependency graph for the lifecycle of [LinkPaymentLauncher] and related
 * classes.
 */
@Singleton
@Component(
    modules = [
        LinkPaymentLauncherModule::class,
        LoggingModule::class
    ]
)
internal abstract class LinkPaymentLauncherComponent {
    abstract fun inject(factory: LinkActivityViewModel.Factory)
    abstract fun inject(factory: SignUpViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun ioContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun analyticsRequestFactory(paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory): Builder

        @BindsInstance
        fun analyticsRequestExecutor(analyticsRequestExecutor: AnalyticsRequestExecutor): Builder

        @BindsInstance
        fun stripeRepository(stripeRepository: StripeRepository): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        fun build(): LinkPaymentLauncherComponent
    }
}
