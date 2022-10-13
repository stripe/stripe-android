package com.stripe.android.paymentsheet.paymentdatacollection.polling.di

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.paymentdatacollection.polling.DefaultTimeProvider
import com.stripe.android.paymentsheet.paymentdatacollection.polling.TimeProvider
import com.stripe.android.polling.DefaultIntentStatusPoller
import com.stripe.android.polling.IntentStatusPoller
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module(subcomponents = [PollingViewModelSubcomponent::class])
internal interface PollingViewModelModule {

    @Binds
    fun bindsIntentStatusPoller(impl: DefaultIntentStatusPoller): IntentStatusPoller

    @Binds
    fun bindsTimeProvider(impl: DefaultTimeProvider): TimeProvider

    companion object {

        @Provides
        fun providesAppContext(application: Application): Context = application

        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
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
}
