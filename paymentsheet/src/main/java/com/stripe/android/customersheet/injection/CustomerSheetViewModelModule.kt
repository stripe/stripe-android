package com.stripe.android.customersheet.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.core.os.LocaleListCompat
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        FormViewModelSubcomponent::class,
    ]
)
internal class CustomerSheetViewModelModule {

    /**
     * Provides a non-singleton PaymentConfiguration.
     *
     * Should be fetched only when it's needed, to allow client to set the publishableKey and
     * stripeAccountId in PaymentConfiguration any time before presenting Customer Sheet.
     *
     * Should always be injected with [Lazy] or [Provider].
     */
    @Provides
    fun paymentConfiguration(application: Application): PaymentConfiguration {
        return PaymentConfiguration.getInstance(application)
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> String = { paymentConfiguration.get().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> String? = { paymentConfiguration.get().stripeAccountId }

    @Provides
    @Named(IS_LIVE_MODE)
    fun isLiveMode(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> Boolean = { paymentConfiguration.get().publishableKey.startsWith("pk_live") }

    @Provides
    fun resources(application: Application): Resources {
        return application.resources
    }

    @Provides
    fun context(application: Application): Context {
        return application
    }

    @Provides
    @IOContext
    fun ioContext(): CoroutineContext {
        return Dispatchers.IO
    }

    @Provides
    fun provideLpmRepository(resources: Resources): LpmRepository {
        return LpmRepository.getInstance(
            LpmRepository.LpmRepositoryArguments(resources)
        )
    }

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("CustomerSheet")

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    fun provideLocale() =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

    @Provides
    fun backstack(
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean
    ): List<CustomerSheetViewState> = listOf(
        CustomerSheetViewState.Loading(
            isLiveMode = isLiveModeProvider()
        )
    )

    @Provides
    fun savedPaymentSelection(): PaymentSelection? = savedPaymentSelection

    private companion object {
        private val savedPaymentSelection: PaymentSelection? = null
    }
}
