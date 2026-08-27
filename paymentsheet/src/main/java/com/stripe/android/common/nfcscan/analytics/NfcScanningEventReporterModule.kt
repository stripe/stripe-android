package com.stripe.android.common.nfcscan.analytics

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal interface NfcScanningEventReporterModule {
    @Binds
    fun bindsNfcScanningEventReporter(
        reporter: DefaultNfcScanningEventReporter,
    ): NfcScanningEventReporter

    @Binds
    fun bindsAnalyticsRequestFactory(
        factory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    companion object {
        @EventPrefix
        @Provides
        fun providesEventPrefix(paymentMethodMetadata: PaymentMethodMetadata): String {
            return when (paymentMethodMetadata.integrationMetadata) {
                is IntegrationMetadata.CustomerSheet -> "cs_"
                else -> "mc_"
            }
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(context: Context): () -> String = {
            PaymentConfiguration.getInstance(context).publishableKey
        }

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PRODUCT_USAGE)
        fun providesProductUsage(): Set<String> = emptySet()

        @Provides
        fun provideLogger(@Named(ENABLE_LOGGING) enabled: Boolean) =
            Logger.getInstance(enabled)
    }
}
