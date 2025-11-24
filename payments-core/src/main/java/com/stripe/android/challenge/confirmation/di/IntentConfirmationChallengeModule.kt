package com.stripe.android.challenge.confirmation.di

import com.stripe.android.BuildConfig
import com.stripe.android.challenge.confirmation.BridgeErrorParams
import com.stripe.android.challenge.confirmation.BridgeErrorParamsJsonParser
import com.stripe.android.challenge.confirmation.BridgeSuccessParams
import com.stripe.android.challenge.confirmation.BridgeSuccessParamsJsonParser
import com.stripe.android.challenge.confirmation.ConfirmationChallengeBridgeHandler
import com.stripe.android.challenge.confirmation.DefaultConfirmationChallengeBridgeHandler
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeArgs
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

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
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindAnalyticsRequestExecutor(
        analyticsRequestExecutor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(args: IntentConfirmationChallengeArgs): () -> String {
            return { args.publishableKey }
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsage(args: IntentConfirmationChallengeArgs): Set<String> {
            return args.productUsage.toSet()
        }
    }
}
