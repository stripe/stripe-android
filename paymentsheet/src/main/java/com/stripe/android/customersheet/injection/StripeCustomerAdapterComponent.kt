package com.stripe.android.customersheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerEphemeralKeyProvider
import com.stripe.android.customersheet.SetupIntentClientSecretProvider
import com.stripe.android.customersheet.StripeCustomerAdapter
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
@Component(
    modules = [
        StripeCustomerAdapterModule::class,
        CustomerSheetDataCommonModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
    ]
)
internal interface StripeCustomerAdapterComponent {
    val stripeCustomerAdapter: StripeCustomerAdapter

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun customerEphemeralKeyProvider(
            customerEphemeralKeyProvider: CustomerEphemeralKeyProvider
        ): Builder

        @BindsInstance
        fun setupIntentClientSecretProvider(
            setupIntentClientSecretProvider: SetupIntentClientSecretProvider?
        ): Builder

        @BindsInstance
        fun paymentMethodTypes(
            paymentMethodTypes: List<String>?
        ): Builder

        fun build(): StripeCustomerAdapterComponent
    }
}

@Module
internal interface StripeCustomerAdapterModule {
    companion object {
        @Provides
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (CustomerEphemeralKey) -> PrefsRepository = { customer ->
            DefaultPrefsRepository(
                appContext,
                customer.customerId,
                workContext
            )
        }
    }
}
