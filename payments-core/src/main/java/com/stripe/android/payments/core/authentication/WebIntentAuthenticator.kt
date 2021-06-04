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
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * [IntentAuthenticator] implementation to redirect to a URL through [PaymentBrowserAuthStarter].
 */
internal class WebIntentAuthenticator(
    private val paymentBrowserAuthStarterFactory: (AuthActivityStarter.Host) -> PaymentBrowserAuthStarter,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val logger: Logger,
    private val enableLogging: Boolean,
    private val uiContext: CoroutineContext
) : IntentAuthenticator {

    override suspend fun authenticate(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String?,
        requestOptions: ApiRequest.Options
    ) {
        val authUrl: String
        val returnUrl: String?
        var shouldCancelSource = false
        var shouldCancelIntentOnUserNavigation = true

        when (val nextActionData = stripeIntent.nextActionData) {
            // can only triggered when `use_stripe_sdk=true`
            is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                authUrl = nextActionData.url
                returnUrl = threeDs1ReturnUrl
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
            stripeIntent,
            StripePaymentController.getRequestCode(stripeIntent),
            stripeIntent.clientSecret.orEmpty(),
            authUrl,
            requestOptions.stripeAccount,
            returnUrl = returnUrl,
            shouldCancelSource = shouldCancelSource,
            shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation
        )
    }

    internal suspend fun beginWebAuth(
        host: AuthActivityStarter.Host,
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
        logger.debug("PaymentBrowserAuthStarter#start()")
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
