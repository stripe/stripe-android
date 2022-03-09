package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Component to inject [PaymentLauncherViewModel.Factory] when the app process is killed and
 * there is no [Injector] available.
 */
@Singleton
@Component(
    modules = [
        PaymentLauncherModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        LoggingModule::class
    ]
)
internal interface PaymentLauncherViewModelFactoryComponent {
    fun inject(factory: PaymentLauncherViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): PaymentLauncherViewModelFactoryComponent
    }
}
