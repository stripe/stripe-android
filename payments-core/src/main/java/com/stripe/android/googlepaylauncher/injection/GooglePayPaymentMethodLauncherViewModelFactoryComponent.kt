package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Component to inject [GooglePayPaymentMethodLauncherViewModel.Factory] when the app process is
 * killed and there is no [Injector] available.
 * This component will create new instances of all dependencies.
 */
@Singleton
@Component(
    modules = [
        GooglePayPaymentMethodLauncherModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class
    ]
)
internal interface GooglePayPaymentMethodLauncherViewModelFactoryComponent {
    val subcomponentBuilder: GooglePayPaymentMethodLauncherViewModelSubcomponent.Builder

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

        @BindsInstance
        fun googlePayConfig(config: GooglePayPaymentMethodLauncher.Config): Builder

        @BindsInstance
        fun cardBrandFilter(cardBrandFilter: CardBrandFilter): Builder

        fun build(): GooglePayPaymentMethodLauncherViewModelFactoryComponent
    }
}
