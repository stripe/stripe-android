package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.RetryDelayModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        Stripe3ds2TransactionModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        RetryDelayModule::class
    ]
)
internal interface Stripe3ds2TransactionViewModelFactoryComponent {
    val subcomponentFactory: Stripe3ds2TransactionViewModelSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            context: Context,
            @BindsInstance
            @Named(ENABLE_LOGGING)
            enableLogging: Boolean,
            @BindsInstance
            @Named(PUBLISHABLE_KEY)
            publishableKeyProvider: () -> String,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
            @BindsInstance
            @Named(IS_INSTANT_APP)
            isInstantApp: Boolean,
        ): Stripe3ds2TransactionViewModelFactoryComponent
    }
}
