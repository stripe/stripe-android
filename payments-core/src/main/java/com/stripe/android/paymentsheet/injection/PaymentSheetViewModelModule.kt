package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.repositories.PaymentMethodsApiRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule {
    /**
     * Provides a non-singleton PaymentConfiguration.
     *
     * Needs to be recalculated whenever needed to allow client to set the publishableKey and
     * stripeAccountId in PaymentConfiguration any time before configuring the FlowController
     * through configureWithPaymentIntent or configureWithSetupIntent.
     *
     * Should always be injected with [Lazy] or [Provider].
     */
    @Provides
    fun providePaymentConfiguration(application: Application): PaymentConfiguration {
        return PaymentConfiguration.getInstance(application)
    }

    // Below are all Singleton instance to be injected into PaymentSheetViewModel

    @Provides
    @Singleton
    fun provideApiRequestOptions(
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ) = ApiRequest.Options(
        apiKey = lazyPaymentConfiguration.get().publishableKey,
        stripeAccount = lazyPaymentConfiguration.get().stripeAccountId
    )

    @Provides
    @Singleton
    fun provideStripeApiRepository(
        application: Application,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ) = StripeApiRepository(
        application,
        { lazyPaymentConfiguration.get().publishableKey }
    )

    @Provides
    @Singleton
    fun provideStripeIntentRepository(
        stripeApiRepository: StripeApiRepository,
        lazyPaymentConfig: Lazy<PaymentConfiguration>
    ): StripeIntentRepository {
        return StripeIntentRepository.Api(
            stripeRepository = stripeApiRepository,
            requestOptions = ApiRequest.Options(
                lazyPaymentConfig.get().publishableKey,
                lazyPaymentConfig.get().stripeAccountId
            ),
            workContext = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun providePaymentMethodsApiRepository(
        stripeApiRepository: StripeApiRepository,
        lazyPaymentConfig: Lazy<PaymentConfiguration>
    ): PaymentMethodsRepository {
        return PaymentMethodsApiRepository(
            stripeRepository = stripeApiRepository,
            publishableKey = lazyPaymentConfig.get().publishableKey,
            stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
            workContext = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun providePaymentFlowResultProcessor(
        application: Application,
        starterArgs: PaymentSheetContract.Args,
        lazyPaymentConfig: Lazy<PaymentConfiguration>,
        stripeApiRepository: StripeApiRepository,
    ): PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>> {
        return when (starterArgs.clientSecret) {
            is PaymentIntentClientSecret -> PaymentIntentFlowResultProcessor(
                application,
                { lazyPaymentConfig.get().publishableKey },
                stripeApiRepository,
                enableLogging = true,
                Dispatchers.IO
            )
            is SetupIntentClientSecret -> SetupIntentFlowResultProcessor(
                application,
                { lazyPaymentConfig.get().publishableKey },
                stripeApiRepository,
                enableLogging = true,
                Dispatchers.IO
            )
        }
    }

    @Provides
    @Singleton
    fun provideGooglePayRepository(
        application: Application,
        starterArgs: PaymentSheetContract.Args
    ): GooglePayRepository {
        return starterArgs.config?.googlePay?.environment?.let { environment ->
            DefaultGooglePayRepository(
                application,
                environment
            )
        } ?: GooglePayRepository.Disabled
    }

    @Provides
    @Singleton
    fun providePrefsRepository(
        application: Application,
        starterArgs: PaymentSheetContract.Args,
        googlePayRepository: GooglePayRepository
    ): PrefsRepository {
        return starterArgs.config?.customer?.let { (id) ->
            DefaultPrefsRepository(
                application,
                customerId = id,
                isGooglePayReady = { googlePayRepository.isReady().first() },
                workContext = Dispatchers.IO
            )
        } ?: PrefsRepository.Noop()
    }

    // TODO: Replace with an actual logger
    @Provides
    @Singleton
    fun provideLogger() = Logger.noop()

    @Provides
    @IOContext
    fun provideWorkContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    fun provideStripePaymentController(
        application: Application,
        stripeApiRepository: StripeApiRepository,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>,
    ): PaymentController {
        return StripePaymentController(
            application,
            { lazyPaymentConfiguration.get().publishableKey },
            stripeApiRepository,
            enableLogging = true
        )
    }
}
