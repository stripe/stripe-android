package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [Module] to provide dependencies for [PaymentLauncher].
 *
 * Notes for the constructor parameters [publishableKeyProvider] and [stripeAccountIdProvider]:
 * 1. When [PaymentLauncher] is used as a public API, [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID] are
 * passed through [PaymentLauncherFactory.create] and are determined during initialization.
 *
 * 2. When [PaymentLauncher] is used by PaymentSheet, [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID]
 * might not be available during initialization, as the client might use
 * [PaymentConfiguration.init] to initialize them after [PaymentLauncher] is initialized.
 *
 * To accommodate both scenarios, two [Provider]s are passed to the [Module], they return the direct
 * value in case 1 and use the [Provider]s created by PaymentSheet's dagger graph in case 2.
 */
@Module
internal class PaymentLauncherModule(
    private val publishableKeyProvider: Provider<String>,
    private val stripeAccountIdProvider: Provider<String?>
) {

    /**
     * Because [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID] might change, each time a new [ApiRequest]
     * is to be send through [StripeRepository], a new [ApiRequest.Options]
     * instance is created with the latest values.
     *
     * Should always be used with [Provider] or [Lazy].
     */
    @Provides
    fun provideApiRequestOptions() = ApiRequest.Options(
        apiKey = publishableKeyProvider.get(),
        stripeAccount = stripeAccountIdProvider.get()
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
        @IOContext ioContext: CoroutineContext
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
        @IOContext ioContext: CoroutineContext
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
