package com.stripe.android.payments.core.injection

import android.content.Context
import com.google.android.instantapps.InstantApps
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
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
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext workContext: CoroutineContext,
        @UIContext uiContext: CoroutineContext,
        threeDs1IntentReturnUrlMap: MutableMap<String, String>,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(PRODUCT_USAGE) productUsage: Set<String>,
        @Named(IS_INSTANT_APP) isInstantApp: Boolean,
        @Named(INCLUDE_PAYMENT_SHEET_AUTHENTICATORS) includePaymentSheetAuthenticators: Boolean,
    ): PaymentAuthenticatorRegistry = DefaultPaymentAuthenticatorRegistry.createInstance(
        context = context,
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
        enableLogging = enableLogging,
        workContext = workContext,
        uiContext = uiContext,
        threeDs1IntentReturnUrlMap = threeDs1IntentReturnUrlMap,
        publishableKeyProvider = publishableKeyProvider,
        productUsage = productUsage,
        isInstantApp = isInstantApp,
        includePaymentSheetAuthenticators = includePaymentSheetAuthenticators,
    )

    @Provides
    @Named(IS_INSTANT_APP)
    fun provideIsInstantApp(context: Context): Boolean {
        return InstantApps.isInstantApp(context)
    }
}
