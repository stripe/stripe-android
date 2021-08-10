package com.stripe.android.payments.core.authentication

import com.stripe.android.Logger
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.model.StripeIntent
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
 * [PaymentAuthenticator] implementation to redirect to a URL through [PaymentBrowserAuthStarter].
 */
@Singleton
@JvmSuppressWildcards
internal class WebIntentAuthenticator @Inject constructor(
    private val paymentBrowserAuthStarterFactory: (AuthActivityStarterHost) -> PaymentBrowserAuthStarter,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @UIContext private val uiContext: CoroutineContext,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>,
) : PaymentAuthenticator<StripeIntent> {

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val authUrl: String
        val returnUrl: String?
        var shouldCancelSource = false
        var shouldCancelIntentOnUserNavigation = true

        when (val nextActionData = authenticatable.nextActionData) {
            // can only triggered when `use_stripe_sdk=true`
            is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                authUrl = nextActionData.url
                returnUrl = authenticatable.id?.let {
                    threeDs1IntentReturnUrlMap.remove(it)
                }
                // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
                shouldCancelSource = true
                analyticsRequestExecutor.executeAsync(
                    analyticsRequestFactory.createRequest(
                        AnalyticsEvent.Auth3ds1Sdk
                    )
                )
            }
            // can only triggered when `use_stripe_sdk=false`
            is StripeIntent.NextActionData.RedirectToUrl -> {
                analyticsRequestExecutor.executeAsync(
                    analyticsRequestFactory.createRequest(AnalyticsEvent.AuthRedirect)
                )
                authUrl = nextActionData.url.toString()
                returnUrl = nextActionData.returnUrl
            }
            is StripeIntent.NextActionData.AlipayRedirect -> {
                analyticsRequestExecutor.executeAsync(
                    analyticsRequestFactory.createRequest(AnalyticsEvent.AuthRedirect)
                )
                authUrl = nextActionData.webViewUrl.toString()
                returnUrl = nextActionData.returnUrl
            }
            is StripeIntent.NextActionData.DisplayOxxoDetails -> {
                // nextActionData.hostedVoucherUrl will never be null as AuthenticatorRegistry won't direct it here
                authUrl = nextActionData.hostedVoucherUrl.takeIf { it!!.isNotEmpty() }
                    ?: throw IllegalArgumentException("null hostedVoucherUrl for DisplayOxxoDetails")
                returnUrl = null
                shouldCancelIntentOnUserNavigation = false
            }
            else ->
                throw IllegalArgumentException("WebAuthenticator can't process nextActionData: $nextActionData")
        }

        beginWebAuth(
            host,
            authenticatable,
            StripePaymentController.getRequestCode(authenticatable),
            authenticatable.clientSecret.orEmpty(),
            authUrl,
            requestOptions.stripeAccount,
            returnUrl = returnUrl,
            shouldCancelSource = shouldCancelSource,
            shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation
        )
    }

    internal suspend fun beginWebAuth(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestCode: Int,
        clientSecret: String,
        authUrl: String,
        stripeAccount: String?,
        returnUrl: String? = null,
        shouldCancelSource: Boolean = false,
        shouldCancelIntentOnUserNavigation: Boolean = true
    ) = withContext(uiContext) {
        val paymentBrowserWebStarter = paymentBrowserAuthStarterFactory(host)
        Logger.getInstance(enableLogging).debug("PaymentBrowserAuthStarter#start()")
        paymentBrowserWebStarter.start(
            PaymentBrowserAuthContract.Args(
                objectId = stripeIntent.id.orEmpty(),
                requestCode,
                clientSecret,
                authUrl,
                returnUrl,
                enableLogging,
                stripeAccountId = stripeAccount,
                shouldCancelSource = shouldCancelSource,
                shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation
            )
        )
    }
}
