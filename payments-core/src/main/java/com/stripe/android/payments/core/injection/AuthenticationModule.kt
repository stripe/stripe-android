package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.authentication.RealRedirectResolver
import com.stripe.android.payments.core.authentication.RedirectResolver
import com.stripe.android.payments.core.authentication.VoucherNextActionHandler
import com.stripe.android.payments.core.authentication.WebIntentNextActionHandler
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Provides mappings between [NextActionData] and [PaymentNextActionHandler] provided by payment SDK.
 */
@Module
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class AuthenticationModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS1::class)
    abstract fun binds3DS1Authenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.RedirectToUrl::class)
    abstract fun bindsRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.AlipayRedirect::class)
    abstract fun bindsAlipayRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayMultibancoDetails::class)
    abstract fun bindsMultibancoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayOxxoDetails::class)
    abstract fun bindsOxxoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayKonbiniDetails::class)
    abstract fun bindsKonbiniAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayBoletoDetails::class)
    abstract fun bindsBoletoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.CashAppRedirect::class)
    abstract fun bindsCashAppRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SwishRedirect::class)
    abstract fun bindsSwishRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @Binds
    abstract fun bindsRedirectResolver(impl: RealRedirectResolver): RedirectResolver

    companion object {
        @Provides
        @Singleton
        fun provideDefaultReturnUrl(
            context: Context
        ) = DefaultReturnUrl.create(context)

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
