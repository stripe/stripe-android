package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.Logger
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentLauncherModule {
    @Provides
    @Singleton
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @Singleton
    fun provideApiRequestOptions(
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountId: String?,
    ) = ApiRequest.Options(
        apiKey = publishableKey,
        stripeAccount = stripeAccountId
    )

    @Provides
    @Singleton
    fun provideThreeDs1IntentReturnUrlMap() = mutableMapOf<String, String>()

    @Provides
    @Singleton
    @IOContext
    fun provideIOContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    @UIContext
    fun provideUIContext(): CoroutineContext = Dispatchers.Main

    @Provides
    @Singleton
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    @Singleton
    fun provideDefaultReturnUrl(context: Context) = DefaultReturnUrl.create(context)

    @Provides
    @Singleton
    fun provideStripeApiRepository(
        context: Context,
        @Named(PUBLISHABLE_KEY) publishableKey: String,
    ): StripeRepository = StripeApiRepository(
        context,
        { publishableKey }
    )

    @Provides
    @Singleton
    fun providePaymentIntentFlowResultProcessor(
        context: Context,
        stripeApiRepository: StripeRepository,
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext ioContext: CoroutineContext
    ): PaymentIntentFlowResultProcessor {
        return PaymentIntentFlowResultProcessor(
            context,
            { publishableKey },
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
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext ioContext: CoroutineContext
    ): SetupIntentFlowResultProcessor {
        return SetupIntentFlowResultProcessor(
            context,
            { publishableKey },
            stripeApiRepository,
            enableLogging = enableLogging,
            ioContext
        )
    }

    @Provides
    @Singleton
    fun provideAnalyticsRequestFactory(
        context: Context,
        @Named(PUBLISHABLE_KEY) publishableKey: String
    ) = AnalyticsRequestFactory(
        context,
        { publishableKey }
    )

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
