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
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        FormViewModelSubcomponent::class,
    ]
)
internal class CustomerSheetViewModelModule {

    @Provides
    fun paymentConfiguration(application: Application): PaymentConfiguration {
        return PaymentConfiguration.getInstance(application)
    }

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
    @Named(PUBLISHABLE_KEY)
    fun publishableKeyProvider(paymentConfiguration: PaymentConfiguration): () -> String {
        return { paymentConfiguration.publishableKey }
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
    fun initialViewState(paymentConfiguration: PaymentConfiguration): CustomerSheetViewState {
        return CustomerSheetViewState.Loading(
            isLiveMode = paymentConfiguration.publishableKey.contains("live")
        )
    }
}
