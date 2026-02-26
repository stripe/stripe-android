package com.stripe.android.challenge.confirmation.di

import com.stripe.android.BuildConfig
import com.stripe.android.challenge.confirmation.BridgeErrorParams
import com.stripe.android.challenge.confirmation.BridgeErrorParamsJsonParser
import com.stripe.android.challenge.confirmation.BridgeSuccessParams
import com.stripe.android.challenge.confirmation.BridgeSuccessParamsJsonParser
import com.stripe.android.challenge.confirmation.ConfirmationChallengeBridgeHandler
import com.stripe.android.challenge.confirmation.DefaultConfirmationChallengeBridgeHandler
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeArgs
import com.stripe.android.challenge.confirmation.analytics.DefaultIntentConfirmationChallengeAnalyticsEventReporter
import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface IntentConfirmationChallengeModule {

    @Binds
    fun bindBridgeHandler(
        bridgeHandler: DefaultConfirmationChallengeBridgeHandler
    ): ConfirmationChallengeBridgeHandler

    @Binds
    fun bindBridgeSuccessParamsParser(
        parser: BridgeSuccessParamsJsonParser
    ): ModelJsonParser<BridgeSuccessParams>

    @Binds
    fun bindBridgeErrorParamsParser(
        parser: BridgeErrorParamsJsonParser
    ): ModelJsonParser<BridgeErrorParams>

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindAnalyticsReporter(
        analyticsReporter: DefaultIntentConfirmationChallengeAnalyticsEventReporter
    ): IntentConfirmationChallengeAnalyticsEventReporter

    @Binds
    fun bindAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Singleton
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsage(args: IntentConfirmationChallengeArgs): Set<String> {
            return args.productUsage.toSet()
        }

        @Provides
        @Named(SDK_USER_AGENT)
        fun providesSdkUserAgent(): String = RequestHeadersFactory.getUserAgent()
    }
}

internal const val SDK_USER_AGENT = "SDK_USER_AGENT"
