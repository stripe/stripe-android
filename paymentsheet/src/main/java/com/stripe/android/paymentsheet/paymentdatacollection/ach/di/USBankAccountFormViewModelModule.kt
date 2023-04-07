package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module(
    subcomponents = [USBankAccountFormViewModelSubcomponent::class]
)
internal class USBankAccountFormViewModelModule {
    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(appContext)
    }

    @Provides
    fun providesResources(appContext: Context): Resources {
        return appContext.resources
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        appContext: Context
    ): () -> String = { PaymentConfiguration.getInstance(appContext).publishableKey }

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
}
