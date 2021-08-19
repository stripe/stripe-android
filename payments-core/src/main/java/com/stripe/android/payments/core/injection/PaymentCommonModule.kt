package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
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
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>
        ) = StripeApiRepository(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey }
        )

        @Provides
        @Singleton
        internal fun provideStripePaymentController(
            appContext: Context,
            stripeApiRepository: StripeApiRepository,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
            @Named(ENABLE_LOGGING) enableLogging: Boolean
        ): PaymentController {
            return StripePaymentController(
                appContext,
                { lazyPaymentConfiguration.get().publishableKey },
                stripeApiRepository,
                enableLogging
            )
        }

        @Provides
        @Singleton
        internal fun providePaymentIntentFlowResultProcessor(
            appContext: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
            stripeApiRepository: StripeApiRepository,
            @Named(ENABLE_LOGGING) enableLogging: Boolean,
            @IOContext workContext: CoroutineContext
        ) = PaymentIntentFlowResultProcessor(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging,
            workContext
        )

        @Provides
        @Singleton
        internal fun provideSetupIntentFlowResultProcessor(
            appContext: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
            stripeApiRepository: StripeApiRepository,
            @Named(ENABLE_LOGGING) enableLogging: Boolean,
            @IOContext workContext: CoroutineContext
        ) = SetupIntentFlowResultProcessor(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging,
            workContext
        )

        /**
         * Fetch the correct [PaymentFlowResultProcessor] based on current [ClientSecret].
         *
         * Should always be injected with [Provider].
         */
        @Provides
        internal fun providePaymentFlowResultProcessor(
            clientSecret: ClientSecret,
            paymentIntentFlowResultProcessor: PaymentIntentFlowResultProcessor,
            setupIntentFlowResultProcessor: SetupIntentFlowResultProcessor
        ): PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>> {
            return when (clientSecret) {
                is PaymentIntentClientSecret -> paymentIntentFlowResultProcessor
                is SetupIntentClientSecret -> setupIntentFlowResultProcessor
            }
        }

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
