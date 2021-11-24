package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.injection.UIContext
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
@Component(
    modules = [
        PaymentLauncherModule::class,
        LoggingModule::class
    ]
)
internal interface PaymentLauncherComponent {
    fun inject(factory: PaymentLauncherViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun ioContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun uiContext(@UIContext uiContext: CoroutineContext): Builder

        @BindsInstance
        fun stripeRepository(stripeRepository: StripeRepository): Builder

        @BindsInstance
        fun analyticsRequestFactory(paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): PaymentLauncherComponent
    }
}
