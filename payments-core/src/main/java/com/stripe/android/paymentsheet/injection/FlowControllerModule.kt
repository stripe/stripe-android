package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.analytics.DefaultDeviceIdRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import javax.inject.Provider
import javax.inject.Singleton

@Module
internal class FlowControllerModule {
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

    // Below are all Singleton instance to be injected into DefaultFlowController

    @Provides
    @Singleton
    fun provideFlowControllerInitializer(appContext: Context): FlowControllerInitializer {
        return DefaultFlowControllerInitializer(
            prefsRepositoryFactory =
            { customerId: String, isGooglePayReady: Boolean ->
                DefaultPrefsRepository(
                    appContext,
                    customerId,
                    { isGooglePayReady },
                    Dispatchers.IO
                )
            },
            isGooglePayReadySupplier =
            { environment ->
                val googlePayRepository = environment?.let {
                    DefaultGooglePayRepository(
                        appContext,
                        it
                    )
                } ?: GooglePayRepository.Disabled
                googlePayRepository.isReady().first()
            },
            workContext = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideEventReporter(
        sessionId: SessionId,
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ): EventReporter {
        return DefaultEventReporter(
            mode = EventReporter.Mode.Custom,
            sessionId,
            DefaultDeviceIdRepository(appContext, Dispatchers.IO),
            AnalyticsRequestExecutor.Default(),
            AnalyticsRequestFactory(
                appContext
            ) { lazyPaymentConfiguration.get().publishableKey },
            Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideSessionId() = SessionId()

    @Provides
    @Singleton
    fun provideViewModel(viewModelStoreOwner: ViewModelStoreOwner): FlowControllerViewModel =
        ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]

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
    fun providePaymentFlowResultProcessor(
        appContext: Context,
        viewModel: FlowControllerViewModel,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
        stripeApiRepository: StripeApiRepository
    ): PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>> {
        return when (viewModel.initData.clientSecret) {
            is PaymentIntentClientSecret -> PaymentIntentFlowResultProcessor(
                appContext,
                { lazyPaymentConfiguration.get().publishableKey },
                stripeApiRepository,
                enableLogging = false,
                Dispatchers.IO
            )
            is SetupIntentClientSecret -> SetupIntentFlowResultProcessor(
                appContext,
                { lazyPaymentConfiguration.get().publishableKey },
                stripeApiRepository,
                enableLogging = false,
                Dispatchers.IO
            )
        }
    }

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
}
