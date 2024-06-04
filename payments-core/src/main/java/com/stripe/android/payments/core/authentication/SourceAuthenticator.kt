package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Source
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.IS_INSTANT_APP
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [PaymentAuthenticator] implementation to authenticate [Source].
 */
@Singleton
@JvmSuppressWildcards
internal class SourceAuthenticator @Inject constructor(
    private val paymentBrowserAuthStarterFactory: (AuthActivityStarterHost) -> PaymentBrowserAuthStarter,
    private val paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @UIContext private val uiContext: CoroutineContext,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(IS_INSTANT_APP) private val isInstantApp: Boolean
) : PaymentAuthenticator<Source>() {

    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: Source,
        requestOptions: ApiRequest.Options
    ) {
        if (authenticatable.flow == Source.Flow.Redirect) {
            startSourceAuth(
                host,
                authenticatable,
                requestOptions
            )
        } else {
            bypassAuth(host, authenticatable, requestOptions.stripeAccount)
        }
    }

    private suspend fun startSourceAuth(
        host: AuthActivityStarterHost,
        source: Source,
        requestOptions: ApiRequest.Options
    ) = withContext(uiContext) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthSourceRedirect)
        )

        val paymentBrowserAuthStarter = paymentBrowserAuthStarterFactory(host)

        paymentBrowserAuthStarter.start(
            PaymentBrowserAuthContract.Args(
                objectId = source.id.orEmpty(),
                requestCode = StripePaymentController.SOURCE_REQUEST_CODE,
                clientSecret = source.clientSecret.orEmpty(),
                url = source.redirect?.url.orEmpty(),
                returnUrl = source.redirect?.returnUrl,
                enableLogging = enableLogging,
                stripeAccountId = requestOptions.stripeAccount,
                statusBarColor = host.statusBarColor,
                publishableKey = publishableKeyProvider(),
                isInstantApp = isInstantApp
            )
        )
    }

    private suspend fun bypassAuth(
        host: AuthActivityStarterHost,
        source: Source,
        stripeAccountId: String?
    ) = withContext(uiContext) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.SourceArgs(source, stripeAccountId)
            )
    }
}
