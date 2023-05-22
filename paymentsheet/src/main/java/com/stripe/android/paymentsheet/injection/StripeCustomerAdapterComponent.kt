package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKeyProvider
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.SetupIntentClientSecretProvider
import com.stripe.android.paymentsheet.repositories.StripeCustomerAdapter
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java.util.Calendar
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
@Component(
    modules = [
        StripeCustomerAdapterModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
    ]
)
@OptIn(ExperimentalCustomerSheetApi::class)
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

        fun build(): StripeCustomerAdapterComponent
    }
}

@Module
@OptIn(ExperimentalCustomerSheetApi::class)
internal interface StripeCustomerAdapterModule {
    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    companion object {
        @Provides
        fun provideTimeProvider(): () -> Long = {
            Calendar.getInstance().timeInMillis
        }

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

        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(configuration: PaymentConfiguration): () -> String = {
            configuration.publishableKey
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun providesProductUsage(): Set<String> = setOf("WalletMode")

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
    }
}
