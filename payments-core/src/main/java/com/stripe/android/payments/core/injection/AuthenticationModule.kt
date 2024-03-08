package com.stripe.android.payments.core.injection

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ErrorReporter
import com.stripe.android.core.networking.FakeErrorReporter
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.networking.QErrorReporter
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.core.authentication.BoletoAuthenticator
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.KonbiniAuthenticator
import com.stripe.android.payments.core.authentication.OxxoAuthenticator
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.RealRedirectResolver
import com.stripe.android.payments.core.authentication.RedirectResolver
import com.stripe.android.payments.core.authentication.WebIntentAuthenticator
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Provides mappings between [NextActionData] and [PaymentAuthenticator] provided by payment SDK.
 */
@Module
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class AuthenticationModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS1::class)
    abstract fun binds3DS1Authenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.RedirectToUrl::class)
    abstract fun bindsRedirectAuthenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.AlipayRedirect::class)
    abstract fun bindsAlipayRedirectAuthenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayOxxoDetails::class)
    abstract fun bindsOxxoAuthenticator(
        oxxoAuthenticator: OxxoAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayKonbiniDetails::class)
    abstract fun bindsKonbiniAuthenticator(
        konbiniAuthenticator: KonbiniAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayBoletoDetails::class)
    abstract fun bindsBoletoAuthenticator(
        boletoAuthenticator: BoletoAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.CashAppRedirect::class)
    abstract fun bindsCashAppRedirectAuthenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SwishRedirect::class)
    abstract fun bindsSwishRedirectAuthenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): PaymentAuthenticator<StripeIntent>

    @Binds
    abstract fun bindsRedirectResolver(impl: RealRedirectResolver): RedirectResolver

    @Binds
    abstract fun bindsErrorReporter(impl : QErrorReporter) : ErrorReporter


    companion object {
        @Provides
        @Singleton
        fun provideDefaultReturnUrl(
            context: Context
        ) = DefaultReturnUrl.create(context)


        // TODO: I feel like I shouldn't have to rewrite this
        @Provides
        internal fun provideAnalyticsRequestFactory(
            context: Context,
        ): AnalyticsRequestFactory = AnalyticsRequestFactory(
            packageManager = context.packageManager,
            packageName = context.packageName.orEmpty(),
            packageInfo = context.packageInfo,
            publishableKeyProvider = { PaymentConfiguration.getInstance(context = context).publishableKey },
            networkTypeProvider = NetworkTypeDetector(context = context)::invoke,
        )


        @Provides
        @Singleton
        fun providePaymentRelayStarterFactory(
            lazyRegistry: Lazy<DefaultPaymentAuthenticatorRegistry>
        ): (AuthActivityStarterHost) -> PaymentRelayStarter =
            { host: AuthActivityStarterHost ->
                lazyRegistry.get().paymentRelayLauncher?.let {
                    PaymentRelayStarter.Modern(it)
                } ?: PaymentRelayStarter.Legacy(host)
            }

        @Provides
        @Singleton
        fun providePaymentBrowserAuthStarterFactory(
            lazyRegistry: Lazy<DefaultPaymentAuthenticatorRegistry>,
            defaultReturnUrl: DefaultReturnUrl
        ): (AuthActivityStarterHost) -> PaymentBrowserAuthStarter =
            { host: AuthActivityStarterHost ->
                lazyRegistry.get().paymentBrowserAuthLauncher?.let {
                    PaymentBrowserAuthStarter.Modern(it)
                } ?: PaymentBrowserAuthStarter.Legacy(
                    host,
                    defaultReturnUrl
                )
            }
    }
}
