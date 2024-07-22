package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.core.nextactionhandler.DefaultPaymentNextActionHandlerRegistry
import com.stripe.android.payments.core.nextactionhandler.PaymentNextActionHandler
import com.stripe.android.payments.core.nextactionhandler.RealRedirectResolver
import com.stripe.android.payments.core.nextactionhandler.RedirectResolver
import com.stripe.android.payments.core.nextactionhandler.VoucherNextActionHandler
import com.stripe.android.payments.core.nextactionhandler.WebIntentNextActionHandler
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
internal abstract class NextActionHandlerModule {
    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.SdkData.Use3DS1::class)
    abstract fun binds3DS1Authenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.RedirectToUrl::class)
    abstract fun bindsRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.AlipayRedirect::class)
    abstract fun bindsAlipayRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.DisplayMultibancoDetails::class)
    abstract fun bindsMultibancoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.DisplayOxxoDetails::class)
    abstract fun bindsOxxoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.DisplayKonbiniDetails::class)
    abstract fun bindsKonbiniAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.DisplayBoletoDetails::class)
    abstract fun bindsBoletoAuthenticator(
        voucherAuthenticator: VoucherNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.CashAppRedirect::class)
    abstract fun bindsCashAppRedirectAuthenticator(
        webIntentAuthenticator: WebIntentNextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    @IntentNextActionHandlerMap
    @Binds
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.SwishRedirect::class)
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
            lazyRegistry: Lazy<DefaultPaymentNextActionHandlerRegistry>
        ): (AuthActivityStarterHost) -> PaymentRelayStarter =
            { host: AuthActivityStarterHost ->
                lazyRegistry.get().paymentRelayLauncher?.let {
                    PaymentRelayStarter.Modern(it)
                } ?: PaymentRelayStarter.Legacy(host)
            }

        @Provides
        @Singleton
        fun providePaymentBrowserAuthStarterFactory(
            lazyRegistry: Lazy<DefaultPaymentNextActionHandlerRegistry>,
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
