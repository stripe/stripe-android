package com.stripe.android.checkout.injection

import android.app.Application
import android.content.Context
import com.stripe.android.common.di.DisplayDensity
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module(includes = [StripeNetworkClientModule::class])
internal object CheckoutModule {
    @Provides
    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    fun provideEventReporter(
        context: Context,
        mode: EventReporter.Mode,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestV2Executor: AnalyticsRequestV2Executor,
        durationProvider: DurationProvider,
        analyticEventCallbackProvider: Provider<AnalyticEventCallback?>,
        @IOContext workContext: CoroutineContext,
        logger: UserFacingLogger,
        paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?>,
        apiConfigurationProvider: Provider<ApiConfiguration.State>,
    ): DefaultEventReporter {
        return DefaultEventReporter(
            context = context,
            mode = mode,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsRequestV2Executor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = context,
                defaultProductUsageTokens = setOf("PaymentElement"),
                apiConfigurationProvider = apiConfigurationProvider,
            ),
            durationProvider = durationProvider,
            analyticEventCallbackProvider = analyticEventCallbackProvider,
            workContext = workContext,
            logger = logger,
            paymentMethodMetadataProvider = paymentMethodMetadataProvider,
        )
    }

    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens(): Set<String> = setOf("Checkout")

    @Provides
    fun provideStripeImageLoader(context: Context): StripeImageLoader = DefaultStripeImageLoader(context)

    @Provides
    @DisplayDensity
    fun provideDisplayDensity(context: Context): Float = context.resources.displayMetrics.density
}
