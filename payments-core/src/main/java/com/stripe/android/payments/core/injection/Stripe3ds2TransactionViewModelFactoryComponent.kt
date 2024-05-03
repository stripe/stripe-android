package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.RetryDelayModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        Stripe3ds2TransactionModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        RetryDelayModule::class
    ]
)
internal interface Stripe3ds2TransactionViewModelFactoryComponent {
    val subcomponentBuilder: Stripe3ds2TransactionViewModelSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun isInstantApp(@Named(IS_INSTANT_APP) isInstantApp: Boolean): Builder

        fun build(): Stripe3ds2TransactionViewModelFactoryComponent
    }
}
