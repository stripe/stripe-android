package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.paymentsheet.PaymentSheetApiRepository
import com.stripe.android.paymentsheet.PaymentSheetPaymentController
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Common module providing payment related dependencies.
 * In order to use this module, [Context], [ClientSecret] and [ENABLE_LOGGING] boolean
 * need to be provided elsewhere.
 */
@Module
internal class PaymentCommonModule {
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
    fun provideStripeApiRepository(
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ) = StripeApiRepository(
        appContext,
        { lazyPaymentConfiguration.get().publishableKey }
    )

    @Provides
    @Singleton
    fun provideStripePaymentController(
        appContext: Context,
        stripeApiRepository: StripeApiRepository,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ): PaymentController {
        return StripePaymentController(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging = true
        )
    }

    @Provides
    @Singleton
    fun providePaymentSheetApiRepository(
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ) = StripeApiRepository(
        appContext,
        { lazyPaymentConfiguration.get().publishableKey }
    ) as PaymentSheetApiRepository

    @Provides
    @Singleton
    fun providePaymentSheetPaymentController(
        appContext: Context,
        stripeApiRepository: StripeApiRepository,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ) = StripePaymentController(
        appContext,
        { lazyPaymentConfiguration.get().publishableKey },
        stripeApiRepository,
        enableLogging = true
    ) as PaymentSheetPaymentController

    @Provides
    @Singleton
    fun providePaymentIntentFlowResultProcessor(
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
        stripeApiRepository: StripeApiRepository,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
    ): PaymentIntentFlowResultProcessor {
        return PaymentIntentFlowResultProcessor(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging = enableLogging,
            Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideSetupIntentFlowResultProcessor(
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
        stripeApiRepository: StripeApiRepository,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
    ): SetupIntentFlowResultProcessor {
        return SetupIntentFlowResultProcessor(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging = enableLogging,
            Dispatchers.IO
        )
    }

    /**
     * Fetch the correct [PaymentFlowResultProcessor] based on current [ClientSecret].
     *
     * Should always be injected with [Provider].
     */
    @Provides
    fun providePaymentFlowResultProcessor(
        clientSecret: ClientSecret,
        paymentIntentFlowResultProcessor: PaymentIntentFlowResultProcessor,
        setupIntentFlowResultProcessor: SetupIntentFlowResultProcessor
    ): PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>> {
        return when (clientSecret) {
            is PaymentIntentClientSecret -> paymentIntentFlowResultProcessor
            is SetupIntentClientSecret -> setupIntentFlowResultProcessor
        }
    }
}
