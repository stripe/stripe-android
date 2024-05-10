package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.core.injection.IS_INSTANT_APP
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
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @UIContext private val uiContext: CoroutineContext,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(IS_INSTANT_APP) private val isInstantApp: Boolean,
    private val defaultReturnUrl: DefaultReturnUrl,
    private val redirectResolver: RedirectResolver,
) : PaymentAuthenticator<StripeIntent>() {

    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val authUrl: String
        val returnUrl: String?
        var shouldCancelSource = false
        var shouldCancelIntentOnUserNavigation = true

        var referrer: String? = null
        var forceInAppWebView = false

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
                    paymentAnalyticsRequestFactory.createRequest(
                        PaymentAnalyticsEvent.Auth3ds1Sdk
                    )
                )
            }
            // can only triggered when `use_stripe_sdk=false`
            is StripeIntent.NextActionData.RedirectToUrl -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthRedirect)
                )

                returnUrl = nextActionData.returnUrl

                if (authenticatable.paymentMethod?.code == PaymentMethod.Type.WeChatPay.code) {
                    val originalUrl = nextActionData.url.toString()

                    authUrl = redirectResolver(originalUrl)
                    shouldCancelIntentOnUserNavigation = false
                    referrer = originalUrl

                    // This is crucial so that we can set the "Referer" field in the web view activity.
                    // WeChat will otherwise fail with an error indicating an incorrect configuration.
                    forceInAppWebView = true
                } else {
                    authUrl = nextActionData.url.toString()
                }
            }
            is StripeIntent.NextActionData.AlipayRedirect -> {
                analyticsRequestExecutor.executeAsync(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthRedirect)
                )
                authUrl = nextActionData.webViewUrl.toString()
                returnUrl = nextActionData.returnUrl
            }
            is StripeIntent.NextActionData.DisplayVoucherDetails -> {
                // nextActionData.hostedVoucherUrl will never be null as AuthenticatorRegistry won't direct it here
                authUrl = nextActionData.hostedVoucherUrl.takeIf { it!!.isNotEmpty() }
                    ?: throw IllegalArgumentException(
                        "null hostedVoucherUrl for ${authenticatable.nextActionType?.code}"
                    )
                returnUrl = null
                shouldCancelIntentOnUserNavigation = false
            }
            is StripeIntent.NextActionData.CashAppRedirect -> {
                authUrl = nextActionData.mobileAuthUrl
                returnUrl = defaultReturnUrl.value
                shouldCancelIntentOnUserNavigation = false
            }
            is StripeIntent.NextActionData.SwishRedirect -> {
                authUrl = nextActionData.mobileAuthUrl
                returnUrl = defaultReturnUrl.value
                shouldCancelIntentOnUserNavigation = false
            }
            else -> {
                throw IllegalArgumentException("WebAuthenticator can't process nextActionData: $nextActionData")
            }
        }

        beginWebAuth(
            host = host,
            stripeIntent = authenticatable,
            requestCode = StripePaymentController.getRequestCode(authenticatable),
            clientSecret = authenticatable.clientSecret.orEmpty(),
            authUrl = authUrl,
            stripeAccount = requestOptions.stripeAccount,
            returnUrl = returnUrl,
            shouldCancelSource = shouldCancelSource,
            shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation,
            referrer = referrer,
            forceInAppWebView = forceInAppWebView,
        )
    }

    private suspend fun beginWebAuth(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestCode: Int,
        clientSecret: String,
        authUrl: String,
        stripeAccount: String?,
        returnUrl: String? = null,
        shouldCancelSource: Boolean = false,
        shouldCancelIntentOnUserNavigation: Boolean = true,
        referrer: String?,
        forceInAppWebView: Boolean,
    ) = withContext(uiContext) {
        val paymentBrowserWebStarter = paymentBrowserAuthStarterFactory(host)
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
                shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation,
                statusBarColor = host.statusBarColor,
                publishableKey = publishableKeyProvider(),
                isInstantApp = isInstantApp,
                referrer = referrer,
                forceInAppWebView = forceInAppWebView,
            )
        )
    }
}
