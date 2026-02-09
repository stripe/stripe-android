package com.stripe.android.payments.core.injection

import android.app.Application
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentLauncherModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoroutineContextModule::class,
        ApplicationContextModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentLauncherViewModelFactoryComponent {

    val viewModelSubcomponentFactory: PaymentLauncherViewModelSubcomponent.Factory

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
            @Named(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS)
            includePaymentSheetNextHandlers: Boolean,
        ): PaymentLauncherViewModelFactoryComponent
    }
}
