package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Common module providing payment related dependencies.
 * In order to use this module, [Context], [ClientSecret] and [ENABLE_LOGGING] boolean
 * need to be provided elsewhere.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
@Module
abstract class PaymentCommonModule {

    @Binds
    @Singleton
    internal abstract fun bindsAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    @Binds
    internal abstract fun bindsStripeRepository(repository: StripeApiRepository): StripeRepository

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    companion object {
        /**
         * Provides a non-singleton PaymentConfiguration.
         *
         * Needs to be re-fetched whenever needed to allow client to set the publishableKey and
         * stripeAccountId in PaymentConfiguration any time before configuring the FlowController
         * through configureWithPaymentIntent or configureWithSetupIntent.
         *
         * Should always be injected with [Lazy] or [Provider].
         */
        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @Singleton
        internal fun provideStripeApiRepository(
            appContext: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
            logger: Logger,
            @IOContext workContext: CoroutineContext,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            @Named(PRODUCT_USAGE) productUsageTokens: Set<String>
        ) = StripeApiRepository(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            logger = logger,
            workContext = workContext,
            productUsageTokens = productUsageTokens,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory
        )

        @Provides
        @Singleton
        fun provideAnalyticsRequestFactory(
            context: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
            @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
        ) = AnalyticsRequestFactory(
            context,
            { lazyPaymentConfiguration.get().publishableKey },
            productUsageTokens,
        )
    }
}
