package com.stripe.android.link.injection

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import kotlin.coroutines.CoroutineContext

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LinkAccountAnalytics

@Module
internal object LinkAccountAnalyticsModule {
    @Provides
    @LinkAccountAnalytics
    fun provideLinkEventsReporter(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        errorReporter: ErrorReporter,
        @IOContext workContext: CoroutineContext,
        logger: Logger,
        durationProvider: DurationProvider,
        configuration: LinkConfiguration,
    ): LinkEventsReporter {
        return DefaultLinkEventsReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
            errorReporter = errorReporter,
            workContext = workContext,
            logger = logger,
            durationProvider = durationProvider,
            apiConfigurationProvider = { configuration.apiConfiguration },
        )
    }
}
