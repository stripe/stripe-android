package com.stripe.android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.stripe.android.Logger

/**
 * A `WebView` used for authenticating payment details
 */
internal class PaymentAuthWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(createContext(context), attrs, defStyleAttr) {
    private var webViewClient: PaymentAuthWebViewClient? = null

    init {
        configureSettings()
    }

    fun init(
        activity: Activity,
        logger: Logger,
        progressBar: ProgressBar,
        clientSecret: String,
        returnUrl: String? = null
    ) {
        webViewClient = PaymentAuthWebViewClient(activity, activity.packageManager, logger,
            progressBar, clientSecret, returnUrl)
        setWebViewClient(webViewClient)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }

    companion object {
        /**
         * Fix for crash in API 21 and 22
         *
         * See <a href="https://stackoverflow.com/q/41025200/">https://stackoverflow.com/q/41025200/</a>
         * for more context.
         */
        fun createContext(context: Context): Context {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.createConfigurationContext(Configuration())
            } else {
                context
            }
        }
    }

    internal class PaymentAuthWebViewClient(
        private val activity: Activity,
        private val packageManager: PackageManager,
        private val logger: Logger,
        private val progressBar: ProgressBar,
        private val clientSecret: String,
        returnUrl: String?
    ) : WebViewClient() {
        // user-specified return URL
        private val userReturnUri: Uri? = if (returnUrl != null) Uri.parse(returnUrl) else null

        var completionUrlParam: String? = null
            private set

        // true if another app was opened from this WebView
        var hasOpenedApp: Boolean = false
            private set

        override fun onPageCommitVisible(view: WebView, url: String) {
            logger.debug("PaymentAuthWebViewClient#onPageCommitVisible() - $url")
            super.onPageCommitVisible(view, url)
            hideProgressBar()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            logger.debug("PaymentAuthWebViewClient#onPageFinished() - $url")
            super.onPageFinished(view, url)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // hide the progress bar here because `onPageCommitVisible()`
                // is not called in API 22 and below
                hideProgressBar()
            }
            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted()
            }
        }

        private fun hideProgressBar() {
            logger.debug("PaymentAuthWebViewClient#hideProgressBar()")
            progressBar.visibility = View.GONE
        }

        private fun isAuthenticateUrl(url: String) = isWhiteListedUrl(url, AUTHENTICATE_URLS)

        private fun isCompletionUrl(url: String) = isWhiteListedUrl(url, COMPLETION_URLS)

        private fun isWhiteListedUrl(url: String, whitelistedUrls: Set<String>): Boolean {
            for (completionUrl in whitelistedUrls) {
                if (url.startsWith(completionUrl)) {
                    return true
                }
            }

            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, urlString: String): Boolean {
            logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading() - $urlString")
            val uri = Uri.parse(urlString)
            updateCompletionUrl(uri)

            return if (isReturnUrl(uri)) {
                logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading() - handle return URL")
                onAuthCompleted()
                true
            } else if ("intent".equals(uri.scheme, ignoreCase = true)) {
                openIntentScheme(uri)
                true
            } else if (!URLUtil.isNetworkUrl(uri.toString())) {
                // Non-network URLs are likely deep-links into banking apps. If the deep-link can be
                // opened via an Intent, start it. Otherwise, stop the authentication attempt.
                openIntent(Intent(Intent.ACTION_VIEW, uri))
                true
            } else {
                super.shouldOverrideUrlLoading(view, urlString)
            }
        }

        private fun openIntentScheme(uri: Uri) {
            logger.debug("PaymentAuthWebViewClient#openIntentScheme()")
            try {
                openIntent(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME))
            } catch (e: Exception) {
                onAuthCompleted()
            }
        }

        private fun openIntent(intent: Intent) {
            logger.debug("PaymentAuthWebViewClient#openIntent()")
            if (intent.resolveActivity(packageManager) != null) {
                hasOpenedApp = true
                activity.startActivity(intent)
            } else {
                // complete auth if the deep-link can't be opened
                onAuthCompleted()
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

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading(WebResourceRequest)")
            return shouldOverrideUrlLoading(view, request.url.toString())
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
            return "stripejs://use_stripe_sdk/return_url" == uri.toString()
        }

        private fun onAuthCompleted() {
            logger.debug("PaymentAuthWebViewClient#onAuthCompleted()")
            activity.finish()
        }

        companion object {
            const val PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret"
            const val PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret"

            private val AUTHENTICATE_URLS = setOf(
                "https://hooks.stripe.com/three_d_secure/authenticate"
            )

            private val COMPLETION_URLS = setOf(
                "https://hooks.stripe.com/redirect/complete/src_",
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_"
            )

            private const val PARAM_RETURN_URL = "return_url"
        }
    }
}
