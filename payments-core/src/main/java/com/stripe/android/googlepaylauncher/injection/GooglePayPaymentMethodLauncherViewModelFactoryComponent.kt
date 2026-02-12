package com.stripe.android.googlepaylauncher.injection

import android.app.Application
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
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
        PaymentElementRequestSurfaceModule::class,
        ApplicationContextModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class
    ]
)
internal interface GooglePayPaymentMethodLauncherViewModelFactoryComponent {
    val subcomponentFactory: GooglePayPaymentMethodLauncherViewModelSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            @Named(ENABLE_LOGGING)
            enableLogging: Boolean,
            @BindsInstance
            @Named(PUBLISHABLE_KEY)
            publishableKeyProvider: () -> String,
            @BindsInstance
            @Named(STRIPE_ACCOUNT_ID)
            stripeAccountIdProvider: () -> String?,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
            @BindsInstance
            config: GooglePayPaymentMethodLauncher.Config,
            @BindsInstance
            cardBrandFilter: CardBrandFilter,
            @BindsInstance
            cardFundingFilter: CardFundingFilter,
        ): GooglePayPaymentMethodLauncherViewModelFactoryComponent
    }
}
