package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Qualifier
import kotlin.coroutines.CoroutineContext

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LinkAccountAnalytics

@Module
internal object LinkAccountAnalyticsModule {
    @Provides
    @LinkAccountAnalytics
    fun providePaymentAnalyticsRequestFactory(
        context: Context,
        configuration: LinkConfiguration,
        @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
    ): PaymentAnalyticsRequestFactory {
        return PaymentAnalyticsRequestFactory(
            context = context,
            publishableKey = configuration.apiConfiguration.publishableKey,
            defaultProductUsageTokens = productUsageTokens,
        )
    }

    @Provides
    @LinkAccountAnalytics
    fun provideLinkEventsReporter(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        @LinkAccountAnalytics paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        errorReporter: ErrorReporter,
        @IOContext workContext: CoroutineContext,
        logger: Logger,
        durationProvider: DurationProvider,
    ): LinkEventsReporter {
        return DefaultLinkEventsReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
            errorReporter = errorReporter,
            workContext = workContext,
            logger = logger,
            durationProvider = durationProvider,
        )
    }
}
