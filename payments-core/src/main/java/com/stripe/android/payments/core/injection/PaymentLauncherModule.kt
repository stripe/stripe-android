package com.stripe.android.payments.core.injection

import android.content.Context
import com.google.android.instantapps.InstantApps
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.Clock
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.SystemClock
import com.stripe.android.payments.core.authentication.DefaultPaymentNextActionHandlerRegistry
import com.stripe.android.payments.core.authentication.PaymentNextActionHandlerRegistry
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [PaymentLauncherViewModelSubcomponent::class]
)
internal class PaymentLauncherModule {
    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        apiConfigurationProvider: () -> ApiConfiguration.State,
    ): () -> String = { apiConfigurationProvider().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        apiConfigurationProvider: () -> ApiConfiguration.State,
    ): () -> String? = { apiConfigurationProvider().stripeAccountId }

    @Provides
    @Singleton
    fun provideThreeDs1IntentReturnUrlMap() = mutableMapOf<String, String>()

    @Provides
    @Singleton
    fun provideDefaultReturnUrl(context: Context) = DefaultReturnUrl.create(context)

    @Provides
    @Singleton
    fun providePaymentNextActionHandlerRegistry(
        context: Context,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext workContext: CoroutineContext,
        @UIContext uiContext: CoroutineContext,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        apiConfigurationProvider: () -> ApiConfiguration.State,
        @Named(PRODUCT_USAGE) productUsage: Set<String>,
        @Named(IS_INSTANT_APP) isInstantApp: Boolean,
        @Named(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS) includePaymentSheetNextHandlers: Boolean,
    ): PaymentNextActionHandlerRegistry = DefaultPaymentNextActionHandlerRegistry.createInstance(
        context = context,
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
        enableLogging = enableLogging,
        workContext = workContext,
        uiContext = uiContext,
        publishableKeyProvider = { apiConfigurationProvider().publishableKey },
        productUsage = productUsage,
        isInstantApp = isInstantApp,
        includePaymentSheetNextActionHandlers = includePaymentSheetNextHandlers,
    )

    @Provides
    @Named(IS_INSTANT_APP)
    fun provideIsInstantApp(context: Context): Boolean {
        return InstantApps.isInstantApp(context)
    }

    @Provides
    @Singleton
    fun provideDurationProvider(): DurationProvider {
        return DefaultDurationProvider.instance
    }

    @Provides
    fun provideClock(): Clock = SystemClock
}
