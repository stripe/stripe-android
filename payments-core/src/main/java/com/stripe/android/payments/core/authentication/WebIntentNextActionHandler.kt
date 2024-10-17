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
 * [PaymentNextActionHandler] implementation to redirect to a URL through [PaymentBrowserAuthStarter].
 */
@Singleton
@JvmSuppressWildcards
internal class WebIntentNextActionHandler @Inject constructor(
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
) : PaymentNextActionHandler<StripeIntent>() {

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val webAuthParams = when (val nextActionData = actionable.nextActionData) {
            // can only triggered when `use_stripe_sdk=true`
            is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                nextActionData.webAuthParams(actionable)
            }
            // can only triggered when `use_stripe_sdk=false`
            is StripeIntent.NextActionData.RedirectToUrl -> {
                nextActionData.webAuthParams(actionable)
            }
            is StripeIntent.NextActionData.AlipayRedirect -> {
                nextActionData.webAuthParams()
            }
            is StripeIntent.NextActionData.DisplayVoucherDetails -> {
                nextActionData.webAuthParams(actionable)
            }
            is StripeIntent.NextActionData.CashAppRedirect -> {
                nextActionData.webAuthParams()
            }
            is StripeIntent.NextActionData.SwishRedirect -> {
                nextActionData.webAuthParams()
            }
            else -> {
                throw IllegalArgumentException("WebAuthenticator can't process nextActionData: $nextActionData")
            }
        }

        beginWebAuth(
            host = host,
            stripeIntent = actionable,
            requestCode = StripePaymentController.getRequestCode(actionable),
            clientSecret = actionable.clientSecret.orEmpty(),
            authUrl = webAuthParams.authUrl,
            stripeAccount = requestOptions.stripeAccount,
            returnUrl = webAuthParams.returnUrl,
            shouldCancelSource = webAuthParams.shouldCancelSource,
            shouldCancelIntentOnUserNavigation = webAuthParams.shouldCancelIntentOnUserNavigation,
            referrer = webAuthParams.referrer,
            forceInAppWebView = webAuthParams.forceInAppWebView,
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

    private fun StripeIntent.NextActionData.SdkData.Use3DS1.webAuthParams(actionable: StripeIntent): WebAuthParams {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(
                PaymentAnalyticsEvent.Auth3ds1Sdk
            )
        )
        return WebAuthParams(
            authUrl = url,
            returnUrl = actionable.id?.let {
                threeDs1IntentReturnUrlMap.remove(it)
            },
            // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
            shouldCancelSource = true
        )
    }

    private suspend fun StripeIntent.NextActionData.RedirectToUrl.webAuthParams(
        actionable: StripeIntent
    ): WebAuthParams {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthRedirect)
        )
        return if (actionable.paymentMethod?.code == PaymentMethod.Type.WeChatPay.code) {
            WebAuthParams(
                authUrl = redirectResolver(url.toString()),
                returnUrl = returnUrl,
                shouldCancelIntentOnUserNavigation = false,
                referrer = url.toString(),
                // This is crucial so that we can set the "Referer" field in the web view activity.
                // WeChat will otherwise fail with an error indicating an incorrect configuration.
                forceInAppWebView = true
            )
        } else {
            WebAuthParams(
                authUrl = url.toString(),
                returnUrl = returnUrl,
            )
        }
    }

    private fun StripeIntent.NextActionData.AlipayRedirect.webAuthParams(): WebAuthParams {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.AuthRedirect)
        )

        return WebAuthParams(
            authUrl = webViewUrl.toString(),
            returnUrl = returnUrl,
        )
    }

    private fun StripeIntent.NextActionData.DisplayVoucherDetails.webAuthParams(
        actionable: StripeIntent
    ): WebAuthParams {
        return WebAuthParams(
            // hostedVoucherUrl will never be null as NextActionHandlerRegistry won't direct it here
            authUrl = hostedVoucherUrl.takeIf { it!!.isNotEmpty() }
                ?: throw IllegalArgumentException(
                    "null hostedVoucherUrl for ${actionable.nextActionType?.code}"
                ),
            returnUrl = null,
            shouldCancelIntentOnUserNavigation = shouldCancelIntentOnUserNavigation
        )
    }

    private fun StripeIntent.NextActionData.CashAppRedirect.webAuthParams(): WebAuthParams {
        return WebAuthParams(
            authUrl = mobileAuthUrl,
            returnUrl = defaultReturnUrl.value,
            shouldCancelIntentOnUserNavigation = false
        )
    }

    private fun StripeIntent.NextActionData.SwishRedirect.webAuthParams(): WebAuthParams {
        return WebAuthParams(
            authUrl = mobileAuthUrl,
            returnUrl = defaultReturnUrl.value,
            shouldCancelIntentOnUserNavigation = false
        )
    }
}

private data class WebAuthParams(
    val authUrl: String,
    val returnUrl: String?,
    val shouldCancelSource: Boolean = false,
    val shouldCancelIntentOnUserNavigation: Boolean = true,
    val referrer: String? = null,
    val forceInAppWebView: Boolean = false,
)
