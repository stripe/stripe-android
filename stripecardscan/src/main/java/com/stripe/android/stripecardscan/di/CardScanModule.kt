package com.stripe.android.stripecardscan.di

import android.app.Application
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.stripecardscan.BuildConfig
import com.stripe.android.stripecardscan.cardscan.CardScanEventsReporter
import com.stripe.android.stripecardscan.cardscan.DefaultCardScanEventsReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface CardScanModule {

    @Binds
    @Singleton
    fun bindCardScanEventReporter(cardScanEventsReporter: DefaultCardScanEventsReporter): CardScanEventsReporter

    companion object {
        @Provides
        @Singleton
        internal fun providesAnalyticsRequestExecutor(
            executor: DefaultAnalyticsRequestExecutor
        ): AnalyticsRequestExecutor = executor

        @Provides
        @Singleton
        internal fun provideAnalyticsRequestFactory(
            application: Application,
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): AnalyticsRequestFactory = AnalyticsRequestFactory(
            packageManager = application.packageManager,
            packageName = application.packageName.orEmpty(),
            packageInfo = application.packageInfo,
            publishableKeyProvider = publishableKeyProvider,
            networkTypeProvider = NetworkTypeDetector(application)::invoke,
        )

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(
            application: Application
        ): () -> String = { PaymentConfiguration.getInstance(application.applicationContext).publishableKey }

        @Provides
        @Singleton
        fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
            Logger.getInstance(enableLogging)
    }
}
