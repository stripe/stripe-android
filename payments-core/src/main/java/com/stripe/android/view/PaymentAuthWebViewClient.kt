package com.stripe.android.view

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.Logger
import com.stripe.android.payments.DefaultReturnUrl
import kotlinx.coroutines.flow.MutableStateFlow

internal class PaymentAuthWebViewClient(
    private val logger: Logger,
    private val isPageLoaded: MutableStateFlow<Boolean>,
    private val clientSecret: String,
    returnUrl: String?,
    private val activityStarter: (Intent) -> Unit,
    private val activityFinisher: (Throwable?) -> Unit
) : WebViewClient() {
    // user-specified return URL
    private val userReturnUri: Uri? = returnUrl?.let { Uri.parse(it) }

    var completionUrlParam: String? = null
        private set

    internal var hasLoadedBlank: Boolean = false

    override fun onPageFinished(view: WebView, url: String?) {
        logger.debug("PaymentAuthWebViewClient#onPageFinished() - $url")
        super.onPageFinished(view, url)

        if (!hasLoadedBlank) {
            // hide the progress bar here because doing it in `onPageCommitVisible()`
            // potentially causes a crash
            hideProgressBar()
        }

        if (url != null && isCompletionUrl(url)) {
            logger.debug("$url is a completion URL")
            onAuthCompleted()
        }
    }

    private fun hideProgressBar() {
        logger.debug("PaymentAuthWebViewClient#hideProgressBar()")

        isPageLoaded.value = true
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url
        logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading(): $url")
        updateCompletionUrl(url)

        return if (isReturnUrl(url)) {
            logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading() - handle return URL")
            onAuthCompleted()
            true
        } else if ("intent".equals(url.scheme, ignoreCase = true)) {
            openIntentScheme(url)
            true
        } else if (!URLUtil.isNetworkUrl(url.toString())) {
            // Non-network URLs are likely deep-links into banking apps. If the deep-link can be
            // opened via an Intent, start it. Otherwise, stop the authentication attempt.
            openIntent(
                Intent(Intent.ACTION_VIEW, url)
            )
            true
        } else {
            super.shouldOverrideUrlLoading(view, request)
        }
    }

    private fun openIntentScheme(uri: Uri) {
        logger.debug("PaymentAuthWebViewClient#openIntentScheme()")
        runCatching {
            openIntent(
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            )
        }.onFailure { error ->
            logger.error("Failed to start Intent.", error)
            onAuthCompleted(error)
        }
    }

    /**
     * See https://developer.android.com/training/basics/intents/package-visibility-use-cases
     * for more details on app-to-app interaction.
     */
    private fun openIntent(
        intent: Intent
    ) {
        logger.debug("PaymentAuthWebViewClient#openIntent()")

        runCatching {
            activityStarter(intent)
        }.onFailure { error ->
            logger.error("Failed to start Intent.", error)

            if (intent.scheme != "alipays") {
                // complete auth if the deep-link can't be opened unless it is Alipay.
                // The Alipay web view tries to open the Alipay app as soon as it is opened
                // irrespective of whether or not the app is installed.
                // If this intent fails to resolve, we should still let the user
                // continue on the mobile site.
                onAuthCompleted(error)
            }
        }
    }

    private fun updateCompletionUrl(uri: Uri) {
        logger.debug("PaymentAuthWebViewClient#updateCompletionUrl()")
        val returnUrlParam = if (isAuthenticateUrl(uri.toString())) {
            uri.getQueryParameter(PARAM_RETURN_URL)
        } else {
            null
        }

        if (!returnUrlParam.isNullOrBlank()) {
            completionUrlParam = returnUrlParam
        }
    }

    private fun isReturnUrl(uri: Uri): Boolean {
        logger.debug("PaymentAuthWebViewClient#isReturnUrl()")
        when {
            isPredefinedReturnUrl(uri) -> return true

            // If the `userReturnUri` is known, look for URIs that match it.
            userReturnUri != null ->
                return userReturnUri.scheme != null &&
                    userReturnUri.scheme == uri.scheme &&
                    userReturnUri.host != null &&
                    userReturnUri.host == uri.host
            else -> {
                // Skip opaque (i.e. non-hierarchical) URIs
                if (uri.isOpaque) {
                    return false
                }

                // If the `userReturnUri` is unknown, look for URIs that contain a
                // `payment_intent_client_secret` or `setup_intent_client_secret`
                // query parameter, and check if its values matches the given `clientSecret`
                // as a query parameter.
                val paramNames = uri.queryParameterNames
                val clientSecret = when {
                    paramNames.contains(PARAM_PAYMENT_CLIENT_SECRET) ->
                        uri.getQueryParameter(PARAM_PAYMENT_CLIENT_SECRET)
                    paramNames.contains(PARAM_SETUP_CLIENT_SECRET) ->
                        uri.getQueryParameter(PARAM_SETUP_CLIENT_SECRET)
                    else -> null
                }
                return this.clientSecret == clientSecret
            }
        }
    }

    // pre-defined return URLs
    private fun isPredefinedReturnUrl(uri: Uri): Boolean {
        return "stripejs://use_stripe_sdk/return_url" == uri.toString() ||
            uri.toString().startsWith(DefaultReturnUrl.PREFIX)
    }

    /**
     * Invoked when authentication flow has completed, whether succeeded or failed.
     */
    private fun onAuthCompleted(
        error: Throwable? = null
    ) {
        logger.debug("PaymentAuthWebViewClient#onAuthCompleted()")
        activityFinisher(error)
    }

    internal companion object {
        internal const val PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret"
        internal const val PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret"

        private val AUTHENTICATE_URLS = setOf(
            "https://hooks.stripe.com/three_d_secure/authenticate"
        )

        private val COMPLETION_URLS = setOf(
            "https://hooks.stripe.com/redirect/complete/",
            "https://hooks.stripe.com/3d_secure/complete/",
            "https://hooks.stripe.com/3d_secure_2/hosted/complete"
        )

        private const val PARAM_RETURN_URL = "return_url"

        internal const val BLANK_PAGE = "about:blank"

        @VisibleForTesting
        internal fun isCompletionUrl(
            url: String
        ): Boolean {
            return COMPLETION_URLS.any(url::startsWith)
        }

        private fun isAuthenticateUrl(
            url: String
        ): Boolean {
            return AUTHENTICATE_URLS.any(url::startsWith)
        }
    }
}
