package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.model.Source
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.UIContext
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
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @UIContext private val uiContext: CoroutineContext
) : PaymentAuthenticator<Source> {

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: Source,
        requestOptions: ApiRequest.Options
    ) {
        if (authenticatable.flow == Source.Flow.Redirect) {
            startSourceAuth(
                paymentBrowserAuthStarterFactory(host),
                authenticatable,
                requestOptions
            )
        } else {
            bypassAuth(host, authenticatable, requestOptions.stripeAccount)
        }
    }

    private suspend fun startSourceAuth(
        paymentBrowserAuthStarter: PaymentBrowserAuthStarter,
        source: Source,
        requestOptions: ApiRequest.Options
    ) = withContext(uiContext) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.AuthSourceRedirect)
        )

        paymentBrowserAuthStarter.start(
            PaymentBrowserAuthContract.Args(
                objectId = source.id.orEmpty(),
                requestCode = StripePaymentController.SOURCE_REQUEST_CODE,
                clientSecret = source.clientSecret.orEmpty(),
                url = source.redirect?.url.orEmpty(),
                returnUrl = source.redirect?.returnUrl,
                enableLogging = enableLogging,
                stripeAccountId = requestOptions.stripeAccount
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
