package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionViewModelFactory
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

/**
 * Component to inject [Stripe3ds2TransactionViewModelFactory] when the app process is killed and
 * there is no [Injector] available.
 */
@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        Stripe3ds2TransactionViewModelModule::class,
        CoroutineContextModule::class
    ]
)
internal interface Stripe3ds2TransactionViewModelFactoryComponent {
    fun inject(stripe3ds2TransactionViewModelFactory: Stripe3ds2TransactionViewModelFactory)

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

        fun build(): Stripe3ds2TransactionViewModelFactoryComponent
    }
}
