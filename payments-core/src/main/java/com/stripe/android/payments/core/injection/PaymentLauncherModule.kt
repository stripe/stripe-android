package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
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
    @Singleton
    fun provideThreeDs1IntentReturnUrlMap() = mutableMapOf<String, String>()

    @Provides
    @Singleton
    fun provideDefaultReturnUrl(context: Context) = DefaultReturnUrl.create(context)

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
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(PRODUCT_USAGE) productUsage: Set<String>
    ): PaymentAuthenticatorRegistry = DefaultPaymentAuthenticatorRegistry.createInstance(
        context,
        stripeRepository,
        defaultAnalyticsRequestExecutor,
        paymentAnalyticsRequestFactory,
        enableLogging,
        workContext,
        uiContext,
        threeDs1IntentReturnUrlMap,
        publishableKeyProvider,
        productUsage
    )
}
