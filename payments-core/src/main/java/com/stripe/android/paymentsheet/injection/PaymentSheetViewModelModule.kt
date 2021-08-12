package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.repositories.PaymentMethodsApiRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal abstract class PaymentSheetViewModelModule {

    @Binds
    abstract fun bindsApplicationForContext(application: Application): Context

    companion object {
        @Provides
        @Singleton
        fun provideClientSecret(
            starterArgs: PaymentSheetContract.Args
        ): ClientSecret {
            return starterArgs.clientSecret
        }

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
        fun provideStripeIntentRepository(
            stripeApiRepository: StripeApiRepository,
            lazyPaymentConfig: Lazy<PaymentConfiguration>,
            @IOContext workContext: CoroutineContext
        ): StripeIntentRepository {
            return StripeIntentRepository.Api(
                stripeRepository = stripeApiRepository,
                requestOptions = ApiRequest.Options(
                    lazyPaymentConfig.get().publishableKey,
                    lazyPaymentConfig.get().stripeAccountId
                ),
                workContext = workContext
            )
        }

        @Provides
        @Singleton
        fun providePaymentMethodsApiRepository(
            stripeApiRepository: StripeApiRepository,
            lazyPaymentConfig: Lazy<PaymentConfiguration>,
            @IOContext workContext: CoroutineContext
        ): PaymentMethodsRepository {
            return PaymentMethodsApiRepository(
                stripeRepository = stripeApiRepository,
                publishableKey = lazyPaymentConfig.get().publishableKey,
                stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                workContext = workContext
            )
        }

        @Provides
        @Singleton
        fun providePrefsRepository(
            appContext: Context,
            starterArgs: PaymentSheetContract.Args,
            @IOContext workContext: CoroutineContext
        ): PrefsRepository {
            return starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    appContext,
                    customerId = id,
                    workContext = workContext
                )
            } ?: PrefsRepository.Noop()
        }

        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Complete

        @Provides
        @Singleton
        fun provideAnalyticsRequestFactory(
            appContext: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>
        ) = AnalyticsRequestFactory(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            setOf(PaymentSheetEvent.PRODUCT_USAGE)
        )
    }
}
