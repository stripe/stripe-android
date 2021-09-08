package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentLauncherModule {

    /**
     * Because [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID] might change, each time a new [ApiRequest]
     * is to be send through [StripeRepository], a new [ApiRequest.Options]
     * instance is created with the latest values.
     *
     * Should always be used with [Provider] or [Lazy].
     */
    @Provides
    fun provideApiRequestOptions(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?
    ) = ApiRequest.Options(
        apiKey = publishableKeyProvider(),
        stripeAccount = stripeAccountIdProvider()
    )

    @Provides
    @Singleton
    fun provideThreeDs1IntentReturnUrlMap() = mutableMapOf<String, String>()

    @Provides
    @Singleton
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    @Singleton
    fun provideDefaultReturnUrl(context: Context) = DefaultReturnUrl.create(context)

    @Provides
    @Singleton
    fun providePaymentIntentFlowResultProcessor(
        context: Context,
        stripeApiRepository: StripeRepository,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext ioContext: CoroutineContext,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    ): PaymentIntentFlowResultProcessor {
        return PaymentIntentFlowResultProcessor(
            context,
            publishableKeyProvider,
            stripeApiRepository,
            enableLogging = enableLogging,
            ioContext
        )
    }

    @Provides
    @Singleton
    fun provideSetupIntentFlowResultProcessor(
        context: Context,
        stripeApiRepository: StripeRepository,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext ioContext: CoroutineContext,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    ): SetupIntentFlowResultProcessor {
        return SetupIntentFlowResultProcessor(
            context,
            publishableKeyProvider,
            stripeApiRepository,
            enableLogging = enableLogging,
            ioContext
        )
    }

    @Provides
    @Singleton
    fun providePaymentAuthenticatorRegistry(
        context: Context,
        stripeRepository: StripeRepository,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext workContext: CoroutineContext,
        @UIContext uiContext: CoroutineContext,
        threeDs1IntentReturnUrlMap: MutableMap<String, String>,
        defaultAnalyticsRequestExecutor: DefaultAnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory
    ): PaymentAuthenticatorRegistry = DefaultPaymentAuthenticatorRegistry.createInstance(
        context,
        stripeRepository,
        defaultAnalyticsRequestExecutor,
        analyticsRequestFactory,
        enableLogging,
        workContext,
        uiContext,
        threeDs1IntentReturnUrlMap,
    )
}
